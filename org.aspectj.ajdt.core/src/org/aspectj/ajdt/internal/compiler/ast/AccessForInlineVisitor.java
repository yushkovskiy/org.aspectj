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

package org.aspectj.ajdt.internal.compiler.ast;


import org.aspectj.ajdt.internal.compiler.lookup.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.weaver.AjcMemberMaker;
import org.aspectj.weaver.ResolvedMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Walks the body of around advice
 * <p/>
 * Makes sure that all member accesses are to public members. Will convert to use access methods when needed to ensure that. This
 * makes it much simpler (and more modular) to inline the body of an around.
 * <p/>
 * ??? constructors are handled different and require access to the target type. changes to
 * org.eclipse.jdt.internal.compiler.ast.AllocationExpression would be required to fix this issue.
 *
 * @author Jim Hugunin
 */

public final class AccessForInlineVisitor extends ASTVisitor {
  @NotNull
  final PrivilegedHandler handler;
  @NotNull
  final AspectDeclaration inAspect;
  @NotNull
  final EclipseFactory world; // alias for inAspect.world
  @NotNull
  private final Map<TypeBinding, Map<FieldBinding, ResolvedMember>> alreadyProcessedReceivers = new HashMap<TypeBinding, Map<FieldBinding, ResolvedMember>>();

  // set to true for ClassLiteralAccess and AssertStatement
  // ??? A better answer would be to transform these into inlinable forms
  public boolean isInlinable = true;

  public AccessForInlineVisitor(@NotNull AspectDeclaration inAspect, @NotNull PrivilegedHandler handler) {
    this.inAspect = inAspect;
    this.world = inAspect.factory;
    this.handler = handler;
  }

  @Override
  public void endVisit(@NotNull SingleNameReference ref, BlockScope scope) {
    if (ref.binding instanceof FieldBinding) {
      ref.binding = getAccessibleField((FieldBinding) ref.binding, ref.actualReceiverType);
    }
  }

  @Override
  public void endVisit(@NotNull QualifiedNameReference ref, BlockScope scope) {
    if (ref.binding instanceof FieldBinding) {
      ref.binding = getAccessibleField((FieldBinding) ref.binding, ref.actualReceiverType);
    }
    if (ref.otherBindings != null && ref.otherBindings.length > 0) {
      TypeBinding receiverType;
      if (ref.binding instanceof FieldBinding) {
        receiverType = ((FieldBinding) ref.binding).type;
      } else if (ref.binding instanceof VariableBinding) {
        receiverType = ((VariableBinding) ref.binding).type;
      } else {
        // !!! understand and fix this case later
        receiverType = ref.otherBindings[0].declaringClass;
      }
      boolean cont = true; // don't continue if we come across a problem
      for (int i = 0, len = ref.otherBindings.length; i < len && cont; i++) {
        final FieldBinding binding = ref.otherBindings[i];
        ref.otherBindings[i] = getAccessibleField(binding, receiverType);
        if (!(binding instanceof ProblemFieldBinding) && binding != null)
          receiverType = binding.type; // TODO Why is this sometimes null?
        else
          cont = false;
      }
    }
  }

  @Override
  public void endVisit(@NotNull FieldReference ref, BlockScope scope) {
    ref.binding = getAccessibleField(ref.binding, ref.actualReceiverType);
  }

  @Override
  public void endVisit(@NotNull MessageSend send, BlockScope scope) {
    if (send instanceof Proceed)
      return;
    if (send.binding == null || !send.binding.isValidBinding())
      return;

    if (send.isSuperAccess() && !send.binding.isStatic()) {
      send.receiver = new ThisReference(send.sourceStart, send.sourceEnd);
      // send.arguments = AstUtil.insert(new ThisReference(send.sourceStart, send.sourceEnd), send.arguments);
      final MethodBinding superAccessBinding = getSuperAccessMethod(send.binding);
      AstUtil.replaceMethodBinding(send, superAccessBinding);
    } else if (!isPublic(send.binding)) {
      send.syntheticAccessor = getAccessibleMethod(send.binding, send.actualReceiverType);
    }

  }

  @Override
  public void endVisit(@NotNull AllocationExpression send, BlockScope scope) {
    if (send.binding == null || !send.binding.isValidBinding())
      return;
    // XXX TBD
    if (isPublic(send.binding))
      return;
    makePublic(send.binding.declaringClass);
    send.binding = handler.getPrivilegedAccessMethod(send.binding, send);
  }

  @Override
  public void endVisit(@NotNull QualifiedTypeReference ref, BlockScope scope) {
    makePublic(ref.resolvedType); // getTypeBinding(scope)); //??? might be trouble
  }

  @Override
  public void endVisit(@NotNull SingleTypeReference ref, BlockScope scope) {
    makePublic(ref.resolvedType); // getTypeBinding(scope)); //??? might be trouble
  }

  @Override
  public void endVisit(@NotNull AssertStatement assertStatement, BlockScope scope) {
    isInlinable = false;
  }

  @Override
  public void endVisit(@NotNull ClassLiteralAccess classLiteral, BlockScope scope) {
    isInlinable = false;
  }

  @Override
  public boolean visit(@NotNull TypeDeclaration localTypeDeclaration, BlockScope scope) {
    // we don't want to transform any local anonymous classes as they won't be inlined
    return false;
  }

  @Nullable
  private FieldBinding getAccessibleField(@Nullable FieldBinding binding, TypeBinding receiverType) {
    // System.err.println("checking field: " + binding);
    if (binding == null || !binding.isValidBinding())
      return binding;

    makePublic(receiverType);
    if (isPublic(binding))
      return binding;
    if (binding instanceof PrivilegedFieldBinding)
      return binding;
    if (binding instanceof InterTypeFieldBinding)
      return binding;

    if (binding.isPrivate() && binding.declaringClass != inAspect.binding) {
      binding.modifiers = AstUtil.makePackageVisible(binding.modifiers);
    }

    // Avoid repeatedly building ResolvedMembers by using info on any done previously in this visitor
    Map<FieldBinding, ResolvedMember> alreadyResolvedMembers = alreadyProcessedReceivers.get(receiverType);
    if (alreadyResolvedMembers == null) {
      alreadyResolvedMembers = new HashMap<FieldBinding, ResolvedMember>();
      alreadyProcessedReceivers.put(receiverType, alreadyResolvedMembers);
    }
    ResolvedMember m = alreadyResolvedMembers.get(binding);
    if (m == null) {
      m = world.makeResolvedMember(binding, receiverType);
      alreadyResolvedMembers.put(binding, m);
    }

    if (inAspect.accessForInline.containsKey(m)) {
      return (FieldBinding) inAspect.accessForInline.get(m);
    }
    final FieldBinding ret = new InlineAccessFieldBinding(inAspect, binding, m);
    inAspect.accessForInline.put(m, ret);
    return ret;
  }

  @NotNull
  private MethodBinding getAccessibleMethod(@NotNull MethodBinding binding, TypeBinding receiverType) {
    if (!binding.isValidBinding())
      return binding;

    makePublic(receiverType); // ???
    if (isPublic(binding))
      return binding;
    if (binding instanceof InterTypeMethodBinding)
      return binding;

    if (binding instanceof ParameterizedMethodBinding) { // pr124999
      binding = binding.original();
    }

    ResolvedMember m = null;
    if (binding.isPrivate() && binding.declaringClass != inAspect.binding) {
      // does this always mean that the aspect is an inner aspect of the bindings
      // declaring class? After all, the field is private but we can see it from
      // where we are.
      binding.modifiers = AstUtil.makePackageVisible(binding.modifiers);
      m = world.makeResolvedMember(binding);
    } else {
      // Sometimes receiverType and binding.declaringClass are *not* the same.

      // Sometimes receiverType is a subclass of binding.declaringClass. In these situations
      // we want the generated inline accessor to call the method on the subclass (at
      // runtime this will be satisfied by the super).
      m = world.makeResolvedMember(binding, receiverType);
    }
    if (inAspect.accessForInline.containsKey(m))
      return (MethodBinding) inAspect.accessForInline.get(m);
    final MethodBinding ret = world.makeMethodBinding(AjcMemberMaker.inlineAccessMethodForMethod(inAspect.typeX, m));
    inAspect.accessForInline.put(m, ret);
    return ret;
  }

  @NotNull
  private MethodBinding getSuperAccessMethod(@NotNull MethodBinding binding) {
    final ResolvedMember m = world.makeResolvedMember(binding);
    final ResolvedMember superAccessMember = AjcMemberMaker.superAccessMethod(inAspect.typeX, m);
    if (inAspect.superAccessForInline.containsKey(superAccessMember)) {
      return ((SuperAccessMethodPair) inAspect.superAccessForInline.get(superAccessMember)).accessMethod;
    }
    final MethodBinding ret = world.makeMethodBinding(superAccessMember);
    inAspect.superAccessForInline.put(superAccessMember, new SuperAccessMethodPair(m, ret));
    return ret;
  }

  private static boolean isPublic(@NotNull FieldBinding fieldBinding) {
    // these are always effectively public to the inliner
    if (fieldBinding instanceof InterTypeFieldBinding)
      return true;
    return fieldBinding.isPublic();
  }

  private static boolean isPublic(@NotNull MethodBinding methodBinding) {
    // these are always effectively public to the inliner
    if (methodBinding instanceof InterTypeMethodBinding)
      return true;
    return methodBinding.isPublic();
  }

  private void makePublic(@Nullable TypeBinding binding) {
    if (binding == null || !binding.isValidBinding())
      return; // has already produced an error
    if (binding instanceof ReferenceBinding) {
      ReferenceBinding rb = (ReferenceBinding) binding;
      if (!rb.isPublic()) {
        try {
          if (rb instanceof ParameterizedTypeBinding) {
            rb = (ReferenceBinding) rb.erasure();
          }
        } catch (Throwable t) { // TODO remove post 1.7.0
          t.printStackTrace();
        }
        handler.notePrivilegedTypeAccess(rb, null); // ???
      }
    } else if (binding instanceof ArrayBinding) {
      makePublic(((ArrayBinding) binding).leafComponentType);
    } else {
      return;
    }
  }

  static final class SuperAccessMethodPair {
    @NotNull
    public final ResolvedMember originalMethod;
    @NotNull
    public final MethodBinding accessMethod;

    public SuperAccessMethodPair(@NotNull ResolvedMember originalMethod, @NotNull MethodBinding accessMethod) {
      this.originalMethod = originalMethod;
      this.accessMethod = accessMethod;
    }
  }

}
