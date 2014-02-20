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
package org.aspectj.ajdt.internal.compiler.lookup;

import org.aspectj.bridge.ISourceLocation;
import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.Map;

public class EclipseTypeMunger extends ConcreteTypeMunger {
  private ResolvedType targetTypeX;
  // protected ReferenceBinding targetBinding = null;
  public AbstractMethodDeclaration sourceMethod;
  private final EclipseFactory world;
  private ISourceLocation sourceLocation;

  public EclipseTypeMunger(EclipseFactory world, ResolvedTypeMunger munger, ResolvedType aspectType,
                           AbstractMethodDeclaration sourceMethod) {
    super(munger, aspectType);
    this.world = world;
    this.sourceMethod = sourceMethod;
    // A null sourceMethod can be because of binary weaving
    if (sourceMethod != null) {
      this.sourceLocation = new EclipseSourceLocation(sourceMethod.compilationResult, sourceMethod.sourceStart,
          sourceMethod.sourceEnd);
      // Piece of magic that tells type mungers where they came from.
      // Won't be persisted unless ResolvedTypeMunger.persistSourceLocation is true.
      munger.setSourceLocation(sourceLocation);

      // use a different ctor for type level inter type decls I think
      // } else {
      // this.sourceLocation = aspectType.getSourceLocation();
      // munger.setSourceLocation(sourceLocation);
    }
    // was
    targetTypeX = munger.getDeclaringType().resolve(world.getWorld());
    // targetTypeX = munger.getSignature().getDeclaringType().resolve(world.getWorld());
    // AMC, needed until generic and raw have distinct sigs...
    if (targetTypeX.isParameterizedType() || targetTypeX.isRawType()) {
      targetTypeX = targetTypeX.getGenericType();
      // targetBinding = (ReferenceBinding)world.makeTypeBinding(targetTypeX);
    }
  }

  public static boolean supportsKind(ResolvedTypeMunger.Kind kind) {
    return kind == ResolvedTypeMunger.Field || kind == ResolvedTypeMunger.Method || kind == ResolvedTypeMunger.Constructor
        || kind == ResolvedTypeMunger.InnerClass;
  }

  public String toString() {
    return "(EclipseTypeMunger " + getMunger() + ")";
  }

  /**
   * Modifies signatures of a TypeBinding through its ClassScope, i.e. adds Method|FieldBindings, plays with inheritance, ...
   */
  public boolean munge(@NotNull SourceTypeBinding sourceType, @NotNull ResolvedType onType) {
    ResolvedType rt = onType;
    if (rt.isRawType() || rt.isParameterizedType()) {
      rt = rt.getGenericType();
    }
    final boolean isExactTargetType = rt.equals(targetTypeX);
    if (!isExactTargetType) {
      // might be the topmost implementor of an interface we care about
      if (munger.getKind() != ResolvedTypeMunger.Method) {
        return false;
      }
      if (onType.isInterface()) {
        return false;
      }
      if (!munger.needsAccessToTopmostImplementor()) {
        return false;
      }
      // so we do need access, and this type could be it...
      if (!onType.isTopmostImplementor(targetTypeX)) {
        return false;
      }
      // we are the topmost implementor of an interface type that needs munging
      // but we only care about public methods here (we only do this at all to
      // drive the JDT MethodVerifier correctly)
      if (!Modifier.isPublic(munger.getSignature().getModifiers())) {
        return false;
      }
    }
    // System.out.println("munging: " + sourceType);
    // System.out.println("match: " + world.fromEclipse(sourceType) +
    // " with " + targetTypeX);
    if (munger.getKind() == ResolvedTypeMunger.Field) {
      mungeNewField(sourceType, (NewFieldTypeMunger) munger);
    } else if (munger.getKind() == ResolvedTypeMunger.Method) {
      return mungeNewMethod(sourceType, onType, (NewMethodTypeMunger) munger, isExactTargetType);
    } else if (munger.getKind() == ResolvedTypeMunger.Constructor) {
      mungeNewConstructor(sourceType, (NewConstructorTypeMunger) munger);
    } else if (munger.getKind() == ResolvedTypeMunger.InnerClass) {
      mungeNewInnerClass(sourceType, onType, (NewMemberClassTypeMunger) munger, isExactTargetType);
    } else {
      throw new RuntimeException("unimplemented: " + munger.getKind());
    }
    return true;
  }

  private boolean mungeNewMethod(@NotNull SourceTypeBinding sourceType, @NotNull ResolvedType onType, @NotNull NewMethodTypeMunger munger,
                                 boolean isExactTargetType) {
    final InterTypeMethodBinding binding = new InterTypeMethodBinding(world, munger, aspectType, sourceMethod);

    if (!isExactTargetType) {
      // we're munging an interface ITD onto a topmost implementor
      final ResolvedMember existingMember = onType.lookupMemberIncludingITDsOnInterfaces(getSignature());
      if (existingMember != null) {
        // already have an implementation, so don't do anything
        if (onType == existingMember.getDeclaringType() && Modifier.isFinal(munger.getSignature().getModifiers())) {
          // final modifier on default implementation is taken to mean that
          // no-one else can provide an implementation
          if (!(sourceType instanceof BinaryTypeBinding)) {
            // If sourceType is a BinaryTypeBinding, this can indicate we are re-applying the ITDs to the target
            // as we 'pull it in' to resolve something.  This means the clash here is with itself !  So if the ITD
            // was final when initially added to the target this error logic will trigger.  We can't easily
            // identify it was added via ITD, so I'm going to make this quick change to say avoid this error for
            // BinaryTypeBindings
            final CompilationUnitScope cuScope = sourceType.scope.compilationUnitScope();
            final MethodBinding offendingBinding = sourceType.getExactMethod(binding.selector, binding.parameters, cuScope);
            sourceType.scope.problemReporter().finalMethodCannotBeOverridden(offendingBinding, binding);
          }
        }
        // so that we find methods from our superinterfaces later on...
        findOrCreateInterTypeMemberFinder(sourceType);
        return false;
      }
    }

    // retain *only* the visibility modifiers and abstract when putting methods on an interface...
    if (sourceType.isInterface()) {
      final boolean isAbstract = (binding.modifiers & ClassFileConstants.AccAbstract) != 0;
      binding.modifiers = (binding.modifiers & (ClassFileConstants.AccPublic | ClassFileConstants.AccProtected | ClassFileConstants.AccPrivate));
      if (isAbstract) {
        binding.modifiers |= ClassFileConstants.AccAbstract;
      }
    }
    if (munger.getSignature().isVarargsMethod()) {
      binding.modifiers |= ClassFileConstants.AccVarargs;
    }
    findOrCreateInterTypeMemberFinder(sourceType).addInterTypeMethod(binding);
    return true;
  }

  // very similar to code in AspectDeclaration
  private boolean mungeNewInnerClass(@NotNull SourceTypeBinding sourceType, ResolvedType onType, @NotNull NewMemberClassTypeMunger munger,
                                     boolean isExactTargetType) {

    final SourceTypeBinding aspectTypeBinding = (SourceTypeBinding) world.makeTypeBinding(aspectType);

    final char[] mungerMemberTypeName = ("$" + munger.getMemberTypeName()).toCharArray();
    ReferenceBinding innerTypeBinding = null;
    for (ReferenceBinding innerType : aspectTypeBinding.memberTypes) {
      final char[] compounded = CharOperation.concatWith(innerType.compoundName, '.');
      if (org.aspectj.org.eclipse.jdt.core.compiler.CharOperation.endsWith(compounded, mungerMemberTypeName)) {
        innerTypeBinding = innerType;
        break;
      }
    }
    // may be unresolved if the aspect type binding was a BinaryTypeBinding
    if (innerTypeBinding instanceof UnresolvedReferenceBinding) {
      innerTypeBinding = (ReferenceBinding) BinaryTypeBinding.resolveType(innerTypeBinding, world.getLookupEnvironment(), true);
    }
    // rb = new InterTypeMemberClassBinding(world, munger, aspectType, aspectTypeBinding, onType, munger.getMemberTypeName(),
    // sourceType);

    // TODO adjust modifier?
    // TODO deal with itd of it onto an interface

    findOrCreateInterTypeMemberClassFinder(sourceType).addInterTypeMemberType(innerTypeBinding);
    return true;
  }

  private void mungeNewConstructor(@NotNull SourceTypeBinding sourceType, @NotNull NewConstructorTypeMunger munger) {
    if (shouldTreatAsPublic()) {
      final MethodBinding binding = world.makeMethodBinding(munger.getSignature(), munger.getTypeVariableAliases());
      findOrCreateInterTypeMemberFinder(sourceType).addInterTypeMethod(binding);
      final TypeVariableBinding[] typeVariables = binding.typeVariables;
      for (int i = 0; i < typeVariables.length; i++) {
        final TypeVariableBinding tv = typeVariables[i];
        final String name = new String(tv.sourceName);
        final TypeVariableBinding[] tv2 = sourceMethod.binding.typeVariables;
        for (int j = 0; j < tv2.length; j++) {
          if (new String(tv2[j].sourceName).equals(name)) {
            typeVariables[i].declaringElement = binding;
          }
        }
      }
      for (int i = 0; i < typeVariables.length; i++) {
        if (typeVariables[i].declaringElement == null) {
          throw new RuntimeException("Declaring element not set");
        }

      }
      // classScope.referenceContext.binding.addMethod(binding);
    } else {
      final InterTypeMethodBinding binding = new InterTypeMethodBinding(world, munger, aspectType, sourceMethod);
      findOrCreateInterTypeMemberFinder(sourceType).addInterTypeMethod(binding);
    }

  }

  private void mungeNewField(@NotNull SourceTypeBinding sourceType, @NotNull NewFieldTypeMunger munger) {
    if (shouldTreatAsPublic() && !targetTypeX.isInterface()) {
      final FieldBinding binding = world.makeFieldBinding(munger);
      findOrCreateInterTypeMemberFinder(sourceType).addInterTypeField(binding);
      // classScope.referenceContext.binding.addField(binding);
    } else {
      final InterTypeFieldBinding binding = new InterTypeFieldBinding(world, munger, aspectType, sourceMethod);
      final InterTypeMemberFinder finder = findOrCreateInterTypeMemberFinder(sourceType);
      // Downgrade this field munger if name is already 'claimed'
      if (finder.definesField(munger.getSignature().getName())) {
        munger.version = NewFieldTypeMunger.VersionOne;
      }
      finder.addInterTypeField(binding);
    }
  }

  private boolean shouldTreatAsPublic() {
    // ??? this is where we could fairly easily choose to treat package-protected
    // ??? introductions like public ones when the target type and the aspect
    // ??? are in the same package
    return Modifier.isPublic(munger.getSignature().getModifiers());
  }

  @NotNull
  private static InterTypeMemberFinder findOrCreateInterTypeMemberFinder(@NotNull SourceTypeBinding sourceType) {
    InterTypeMemberFinder finder = (InterTypeMemberFinder) sourceType.memberFinder;
    if (finder == null) {
      finder = new InterTypeMemberFinder();
      sourceType.memberFinder = finder;
      finder.sourceTypeBinding = sourceType;
    }
    return finder;
  }

  @NotNull
  private static IntertypeMemberTypeFinder findOrCreateInterTypeMemberClassFinder(@NotNull SourceTypeBinding sourceType) {
    IntertypeMemberTypeFinder finder = (IntertypeMemberTypeFinder) sourceType.typeFinder;
    if (finder == null) {
      finder = new IntertypeMemberTypeFinder();
      sourceType.typeFinder = finder;
      finder.targetTypeBinding = sourceType;
      sourceType.tagBits &= ~TagBits.HasNoMemberTypes; // ensure it thinks it has one
    }
    return finder;
  }

  @Override
  public ISourceLocation getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(ISourceLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  /**
   * @return AbstractMethodDeclaration
   */
  public AbstractMethodDeclaration getSourceMethod() {
    return sourceMethod;
  }

  @Override
  @NotNull
  public ConcreteTypeMunger parameterizedFor(ResolvedType target) {
    return new EclipseTypeMunger(world, munger.parameterizedFor(target), aspectType, sourceMethod);
  }

  @Override
  @NotNull
  public ConcreteTypeMunger parameterizeWith(Map m, World w) {
    return new EclipseTypeMunger(world, munger.parameterizeWith(m, w), aspectType, sourceMethod);
  }

}
