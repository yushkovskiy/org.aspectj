/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
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

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.ISourceLocation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class NewConstructorTypeMunger extends ResolvedTypeMunger {
  private final ResolvedMember syntheticConstructor;
  private ResolvedMember explicitConstructor;

  public NewConstructorTypeMunger(ResolvedMember signature, ResolvedMember syntheticConstructor,
                                  ResolvedMember explicitConstructor, Set superMethodsCalled, List typeVariableAliases) {
    super(Constructor, signature);
    this.syntheticConstructor = syntheticConstructor;
    this.typeVariableAliases = typeVariableAliases;
    this.explicitConstructor = explicitConstructor;
    this.setSuperMethodsCalled(superMethodsCalled);

  }

  public boolean equals(Object other) {
    if (!(other instanceof NewConstructorTypeMunger)) {
      return false;
    }
    final NewConstructorTypeMunger o = (NewConstructorTypeMunger) other;
    return ((syntheticConstructor == null) ? (o.syntheticConstructor == null) : syntheticConstructor
        .equals(o.syntheticConstructor))
        & ((explicitConstructor == null) ? (o.explicitConstructor == null) : explicitConstructor
        .equals(o.explicitConstructor));
  }

  // pr262218 - equivalence ignores the explicit constructor since that won't have yet been set for an EclipseTypeMunger
  public boolean equivalentTo(Object other) {
    if (!(other instanceof NewConstructorTypeMunger)) {
      return false;
    }
    final NewConstructorTypeMunger o = (NewConstructorTypeMunger) other;
    return ((syntheticConstructor == null) ? (o.syntheticConstructor == null) : syntheticConstructor
        .equals(o.syntheticConstructor));
  }

  private volatile int hashCode = 0;

  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + ((syntheticConstructor == null) ? 0 : syntheticConstructor.hashCode());
      result = 37 * result + ((explicitConstructor == null) ? 0 : explicitConstructor.hashCode());
      hashCode = result;
    }
    return hashCode;
  }

  // doesnt seem required....
  // public ResolvedMember getDispatchMethod(UnresolvedType aspectType) {
  // return AjcMemberMaker.interMethodBody(signature, aspectType);
  // }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    kind.write(s);
    signature.write(s);
    syntheticConstructor.write(s);
    explicitConstructor.write(s);
    writeSuperMethodsCalled(s);
    writeSourceLocation(s);
    writeOutTypeAliases(s);
  }

  public static ResolvedTypeMunger readConstructor(VersionedDataInputStream s, ISourceContext context) throws IOException {
    ISourceLocation sloc = null;
    final ResolvedMember sig = ResolvedMemberImpl.readResolvedMember(s, context);
    final ResolvedMember syntheticCtor = ResolvedMemberImpl.readResolvedMember(s, context);
    final ResolvedMember explicitCtor = ResolvedMemberImpl.readResolvedMember(s, context);
    final Set superMethodsCalled = readSuperMethodsCalled(s);
    sloc = readSourceLocation(s);
    final List typeVarAliases = readInTypeAliases(s);
    final ResolvedTypeMunger munger = new NewConstructorTypeMunger(sig, syntheticCtor, explicitCtor, superMethodsCalled,
        typeVarAliases);
    if (sloc != null) {
      munger.setSourceLocation(sloc);
    }
    return munger;
  }

  public ResolvedMember getExplicitConstructor() {
    return explicitConstructor;
  }

  public ResolvedMember getSyntheticConstructor() {
    return syntheticConstructor;
  }

  public void setExplicitConstructor(ResolvedMember explicitConstructor) {
    this.explicitConstructor = explicitConstructor;
    // reset hashCode so that its recalculated with new value
    hashCode = 0;
  }

  @Override
  public ResolvedMember getMatchingSyntheticMember(Member member, ResolvedType aspectType) {
    final ResolvedMember ret = getSyntheticConstructor();
    if (ResolvedType.matches(ret, member)) {
      return getSignature();
    }
    return super.getMatchingSyntheticMember(member, aspectType);
  }

  public void check(World world) {
    if (getSignature().getDeclaringType().resolve(world).isAspect()) {
      world.showMessage(IMessage.ERROR, WeaverMessages.format(WeaverMessages.ITD_CONS_ON_ASPECT), getSignature()
          .getSourceLocation(), null);
    }
  }

  /**
   * see ResolvedTypeMunger.parameterizedFor(ResolvedType)
   */
  @Override
  public ResolvedTypeMunger parameterizedFor(ResolvedType target) {
    ResolvedType genericType = target;
    if (target.isRawType() || target.isParameterizedType()) {
      genericType = genericType.getGenericType();
    }
    ResolvedMember parameterizedSignature = null;
    // If we are parameterizing it for a generic type, we just need to 'swap the letters' from the ones used
    // in the original ITD declaration to the ones used in the actual target type declaration.
    if (target.isGenericType()) {
      final TypeVariable[] vars = target.getTypeVariables();
      final UnresolvedTypeVariableReferenceType[] varRefs = new UnresolvedTypeVariableReferenceType[vars.length];
      for (int i = 0; i < vars.length; i++) {
        varRefs[i] = new UnresolvedTypeVariableReferenceType(vars[i]);
      }
      parameterizedSignature = getSignature().parameterizedWith(varRefs, genericType, true, typeVariableAliases);
    } else {
      // For raw and 'normal' parameterized targets (e.g. Interface, Interface<String>)
      parameterizedSignature = getSignature().parameterizedWith(target.getTypeParameters(), genericType,
          target.isParameterizedType(), typeVariableAliases);
    }
    final NewConstructorTypeMunger nctm = new NewConstructorTypeMunger(parameterizedSignature, syntheticConstructor,
        explicitConstructor, getSuperMethodsCalled(), typeVariableAliases);
    nctm.setSourceLocation(getSourceLocation());
    return nctm;
  }

}
