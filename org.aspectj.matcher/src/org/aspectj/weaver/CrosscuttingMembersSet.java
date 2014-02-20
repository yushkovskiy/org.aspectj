/* *******************************************************************
 * Copyright (c) 2002-2009 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 * ******************************************************************/

package org.aspectj.weaver;

import org.aspectj.weaver.patterns.*;
import org.aspectj.weaver.tools.Trace;
import org.aspectj.weaver.tools.TraceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.*;

/**
 * This holds on to all CrosscuttingMembers for a world. It handles management of change.
 *
 * @author Jim Hugunin
 * @author Andy Clement
 */
public class CrosscuttingMembersSet {
  @NotNull
  private static final Trace trace = TraceFactory.getTraceFactory().getTrace(CrosscuttingMembersSet.class);
  @NotNull
  private final transient World world;

  // FIXME AV - ? we may need a sequencedHashMap there to ensure source based precedence for @AJ advice
  @NotNull
  private final Map /* ResolvedType (the aspect) > CrosscuttingMembers */<ResolvedType, CrosscuttingMembers> members = new HashMap<ResolvedType, CrosscuttingMembers>();

  // List of things to be verified once the type system is 'complete'
  @Nullable
  private transient List /* IVerificationRequired */<IVerificationRequired> verificationList = null;

  @Nullable
  private List<ShadowMunger> shadowMungers = null;
  @Nullable
  private List<ConcreteTypeMunger> typeMungers = null;
  @Nullable
  private List<ConcreteTypeMunger> lateTypeMungers = null;
  @Nullable
  private List<DeclareSoft> declareSofts = null;
  @Nullable
  private List<DeclareParents> declareParents = null;
  @Nullable
  private List<DeclareAnnotation> declareAnnotationOnTypes = null;
  @Nullable
  private List<DeclareAnnotation> declareAnnotationOnFields = null;
  @Nullable
  private List<DeclareAnnotation> declareAnnotationOnMethods = null; // includes constructors
  @Nullable
  private List<DeclareTypeErrorOrWarning> declareTypeEows = null;
  @Nullable
  private List<Declare> declareDominates = null;
  private boolean changedSinceLastReset = false;

  public int serializationVersion = 1;

  public CrosscuttingMembersSet(@NotNull World world) {
    this.world = world;
  }

  public boolean addOrReplaceAspect(@NotNull ResolvedType aspectType) {
    return addOrReplaceAspect(aspectType, true);
  }

  /**
   * @return whether or not that was a change to the global signature XXX for efficiency we will need a richer representation than
   * this
   */
  public boolean addOrReplaceAspect(@NotNull ResolvedType aspectType, boolean inWeavingPhase) {
    if (!world.isAspectIncluded(aspectType) || world.hasUnsatisfiedDependency(aspectType)) {
      return false;
    }

    boolean change = false;
    final CrosscuttingMembers xcut = members.get(aspectType);
    if (xcut == null) {
      members.put(aspectType, aspectType.collectCrosscuttingMembers(inWeavingPhase));
      clearCaches();
      change = true;
    } else {
      if (xcut.replaceWith(aspectType.collectCrosscuttingMembers(inWeavingPhase), inWeavingPhase)) {
        clearCaches();
        change = true;
      } else {
        if (inWeavingPhase) {
          // bug 134541 - even though we haven't changed we may have updated the
          // sourcelocation for the shadowMunger which we need to pick up
          shadowMungers = null;
        }
        change = false;
      }
    }
    if (aspectType.isAbstract()) {
      // we might have sub-aspects that need to re-collect their crosscutting members from us
      final boolean ancestorChange = addOrReplaceDescendantsOf(aspectType, inWeavingPhase);
      change = change || ancestorChange;
    }
    changedSinceLastReset = changedSinceLastReset || change;

    return change;
  }

  public void addAdviceLikeDeclares(ResolvedType aspectType) {
    if (!members.containsKey(aspectType)) {
      return;
    }
    final CrosscuttingMembers xcut = members.get(aspectType);
    xcut.addDeclares(aspectType.collectDeclares(true));
  }

  public boolean deleteAspect(UnresolvedType aspectType) {
    final boolean isAspect = members.remove(aspectType) != null;
    clearCaches();
    return isAspect;
  }

  public boolean containsAspect(UnresolvedType aspectType) {
    return members.containsKey(aspectType);
  }

  // XXX only for testing
  @TestOnly
  public void addFixedCrosscuttingMembers(ResolvedType aspectType) {
    members.put(aspectType, aspectType.crosscuttingMembers);
    clearCaches();
  }

  @NotNull
  public List<ShadowMunger> getShadowMungers() {
    if (shadowMungers != null)
      return shadowMungers;
    final List<ShadowMunger> ret = new ArrayList<ShadowMunger>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getShadowMungers());
    }
    shadowMungers = ret;
    return shadowMungers;
  }

  @NotNull
  public List<ConcreteTypeMunger> getTypeMungers() {
    if (typeMungers != null)
      return typeMungers;
    final List<ConcreteTypeMunger> ret = new ArrayList<ConcreteTypeMunger>();
    for (CrosscuttingMembers xmembers : members.values()) {
      // With 1.6.9 there is a change that enables use of more optimal accessors (accessors for private fields).
      // Here is where we determine if two aspects are asking for access to the same field. If they are
      // and
      // In the new style multiple aspects can share the same privileged accessors, so here we check if
      // two aspects are asking for access to the same field. If they are then we don't add a duplicate
      // accessor.
      for (ConcreteTypeMunger mungerToAdd : xmembers.getTypeMungers()) {
        final ResolvedTypeMunger resolvedMungerToAdd = mungerToAdd.getMunger();
        if (isNewStylePrivilegedAccessMunger(resolvedMungerToAdd)) {
          final String newFieldName = resolvedMungerToAdd.getSignature().getName();
          boolean alreadyExists = false;
          for (ConcreteTypeMunger existingMunger : ret) {
            final ResolvedTypeMunger existing = existingMunger.getMunger();
            if (isNewStylePrivilegedAccessMunger(existing)) {
              final String existingFieldName = existing.getSignature().getName();
              if (existingFieldName.equals(newFieldName)
                  && existing.getSignature().getDeclaringType().equals(
                  resolvedMungerToAdd.getSignature().getDeclaringType())) {
                alreadyExists = true;
                break;
              }
            }
          }
          if (!alreadyExists) {
            ret.add(mungerToAdd);
          }
        } else {
          ret.add(mungerToAdd);
        }
      }
    }
    typeMungers = ret;
    return typeMungers;
  }

  /**
   * Retrieve a subset of all known mungers, those of a specific kind.
   *
   * @param kind the kind of munger requested
   * @return a list of those mungers (list is empty if none found)
   */
  @NotNull
  public List<ConcreteTypeMunger> getTypeMungersOfKind(ResolvedTypeMunger.Kind kind) {
    List<ConcreteTypeMunger> collected = null;
    for (ConcreteTypeMunger typeMunger : typeMungers) {
      if (typeMunger.getMunger() != null && typeMunger.getMunger().getKind() == kind) {
        if (collected == null) {
          collected = new ArrayList<ConcreteTypeMunger>();
        }
        collected.add(typeMunger);
      }
    }
    if (collected == null) {
      return Collections.emptyList();
    } else {
      return collected;
    }
  }

  @NotNull
  public List<ConcreteTypeMunger> getLateTypeMungers() {
    if (lateTypeMungers != null)
      return lateTypeMungers;
    final List<ConcreteTypeMunger> ret = new ArrayList<ConcreteTypeMunger>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getLateTypeMungers());
    }
    lateTypeMungers = ret;
    return lateTypeMungers;
  }

  @NotNull
  public List<DeclareSoft> getDeclareSofts() {
    if (declareSofts != null)
      return declareSofts;
    final Set<DeclareSoft> ret = new HashSet<DeclareSoft>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareSofts());
    }
    declareSofts = new ArrayList<DeclareSoft>();
    declareSofts.addAll(ret);
    return declareSofts;
  }

  @NotNull
  public List<DeclareParents> getDeclareParents() {
    if (declareParents != null)
      return declareParents;
    final Set<DeclareParents> ret = new HashSet<DeclareParents>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareParents());
    }
    declareParents = new ArrayList<DeclareParents>();
    declareParents.addAll(ret);
    return declareParents;
  }

  /**
   * @return an amalgamation of the declare @type statements.
   */
  @NotNull
  public List<DeclareAnnotation> getDeclareAnnotationOnTypes() {
    if (declareAnnotationOnTypes != null)
      return declareAnnotationOnTypes;
    final Set<DeclareAnnotation> ret = new LinkedHashSet<DeclareAnnotation>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareAnnotationOnTypes());
    }
    declareAnnotationOnTypes = new ArrayList<DeclareAnnotation>();
    declareAnnotationOnTypes.addAll(ret);
    return declareAnnotationOnTypes;
  }

  /**
   * @return an amalgamation of the declare @field statements.
   */
  @NotNull
  public List<DeclareAnnotation> getDeclareAnnotationOnFields() {
    if (declareAnnotationOnFields != null)
      return declareAnnotationOnFields;
    final Set<DeclareAnnotation> ret = new LinkedHashSet<DeclareAnnotation>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareAnnotationOnFields());
    }
    declareAnnotationOnFields = new ArrayList<DeclareAnnotation>();
    declareAnnotationOnFields.addAll(ret);
    return declareAnnotationOnFields;
  }

  /**
   * @return an amalgamation of the declare @method/@constructor statements.
   */
  @NotNull
  public List<DeclareAnnotation> getDeclareAnnotationOnMethods() {
    if (declareAnnotationOnMethods != null)
      return declareAnnotationOnMethods;
    final Set<DeclareAnnotation> ret = new LinkedHashSet<DeclareAnnotation>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareAnnotationOnMethods());
    }
    declareAnnotationOnMethods = new ArrayList<DeclareAnnotation>();
    declareAnnotationOnMethods.addAll(ret);
    return declareAnnotationOnMethods;
  }

  /**
   * Return an amalgamation of the declare type eow statements
   */
  @NotNull
  public List<DeclareTypeErrorOrWarning> getDeclareTypeEows() {
    if (declareTypeEows != null)
      return declareTypeEows;
    final Set<DeclareTypeErrorOrWarning> ret = new HashSet<DeclareTypeErrorOrWarning>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareTypeErrorOrWarning());
    }
    declareTypeEows = new ArrayList<DeclareTypeErrorOrWarning>();
    declareTypeEows.addAll(ret);
    return declareTypeEows;
  }

  @NotNull
  public List<Declare> getDeclareDominates() {
    if (declareDominates != null)
      return declareDominates;
    final List<Declare> ret = new ArrayList<Declare>();
    for (final Iterator<CrosscuttingMembers> i = members.values().iterator(); i.hasNext(); ) {
      ret.addAll(i.next().getDeclareDominates());
    }
    declareDominates = ret;
    return declareDominates;
  }

  @Nullable
  public ResolvedType findAspectDeclaringParents(@NotNull DeclareParents p) {
    final Set<ResolvedType> keys = this.members.keySet();
    for (final Iterator<ResolvedType> iter = keys.iterator(); iter.hasNext(); ) {
      final ResolvedType element = iter.next();
      for (final Iterator i = members.get(element).getDeclareParents().iterator(); i.hasNext(); ) {
        final DeclareParents dp = (DeclareParents) i.next();
        if (dp.equals(p)) {
          return element;
        }
      }
    }
    return null;
  }

  public void reset() {
    verificationList = null;
    changedSinceLastReset = false;
  }

  public boolean hasChangedSinceLastReset() {
    return changedSinceLastReset;
  }

  /**
   * Record something that needs verifying when we believe the type system is complete. Used for things that can't be verified as
   * we go along - for example some recursive type variable references (pr133307)
   */
  public void recordNecessaryCheck(IVerificationRequired verification) {
    if (verificationList == null) {
      verificationList = new ArrayList<IVerificationRequired>();
    }
    verificationList.add(verification);
  }

  /**
   * Called when type bindings are complete - calls all registered verification objects in turn.
   */
  public void verify() {
    if (verificationList == null) {
      return;
    }
    for (final Iterator<IVerificationRequired> iter = verificationList.iterator(); iter.hasNext(); ) {
      final IVerificationRequired element = iter.next();
      element.verify();
    }
    verificationList = null;
  }

  public void write(@NotNull CompressingDataOutputStream stream) throws IOException {
    // stream.writeInt(serializationVersion);
    stream.writeInt(shadowMungers.size());
    for (final Iterator iterator = shadowMungers.iterator(); iterator.hasNext(); ) {
      final ShadowMunger shadowMunger = (ShadowMunger) iterator.next();
      shadowMunger.write(stream);
    }
    // // private List /* ShadowMunger */shadowMungers = null;
    // // private List typeMungers = null;
    // // private List lateTypeMungers = null;
    // // private List declareSofts = null;
    // // private List declareParents = null;
    // // private List declareAnnotationOnTypes = null;
    // // private List declareAnnotationOnFields = null;
    // // private List declareAnnotationOnMethods = null; // includes constructors
    // // private List declareDominates = null;
    // // private boolean changedSinceLastReset = false;
    //
  }

  private boolean addOrReplaceDescendantsOf(ResolvedType aspectType, boolean inWeavePhase) {
    // System.err.println("Looking at descendants of "+aspectType.getName());
    final Set<ResolvedType> knownAspects = members.keySet();
    final Set<ResolvedType> toBeReplaced = new HashSet<ResolvedType>();
    for (final Iterator<ResolvedType> it = knownAspects.iterator(); it.hasNext(); ) {
      final ResolvedType candidateDescendant = it.next();
      if ((candidateDescendant != aspectType) && (aspectType.isAssignableFrom(candidateDescendant))) {
        toBeReplaced.add(candidateDescendant);
      }
    }
    boolean change = false;
    for (final Iterator<ResolvedType> it = toBeReplaced.iterator(); it.hasNext(); ) {
      final ResolvedType next = it.next();
      final boolean thisChange = addOrReplaceAspect(next, inWeavePhase);
      change = change || thisChange;
    }
    return change;
  }

  private void clearCaches() {
    shadowMungers = null;
    typeMungers = null;
    lateTypeMungers = null;
    declareSofts = null;
    declareParents = null;
    declareAnnotationOnFields = null;
    declareAnnotationOnMethods = null;
    declareAnnotationOnTypes = null;
    declareDominates = null;
  }

  /**
   * Determine if the type munger is: (1) for privileged access (2) for a normally non visible field (3) is from an aspect wanting
   * 'old style' (ie. long) accessor names
   */
  private static boolean isNewStylePrivilegedAccessMunger(ResolvedTypeMunger typeMunger) {
    final boolean b = (typeMunger != null && typeMunger.getKind() == ResolvedTypeMunger.PrivilegedAccess && typeMunger.getSignature()
        .getKind() == Member.FIELD);
    if (!b) {
      return b;
    }
    final PrivilegedAccessMunger privAccessMunger = (PrivilegedAccessMunger) typeMunger;
    return privAccessMunger.shortSyntax;
  }
}
