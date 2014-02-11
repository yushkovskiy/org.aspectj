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

import org.aspectj.bridge.ISourceLocation;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NewMethodTypeMunger extends ResolvedTypeMunger {

  public static ResolvedTypeMunger readMethod(VersionedDataInputStream s, ISourceContext context) throws IOException {
    ISourceLocation sloc = null;
    final ResolvedMemberImpl rmImpl = ResolvedMemberImpl.readResolvedMember(s, context);
    final Set<ResolvedMember> superMethodsCalled = readSuperMethodsCalled(s);
    sloc = readSourceLocation(s);
    final List<String> typeVarAliases = readInTypeAliases(s);

    final ResolvedTypeMunger munger = new NewMethodTypeMunger(rmImpl, superMethodsCalled, typeVarAliases);
    if (sloc != null) {
      munger.setSourceLocation(sloc);
    }
    return munger;
  }

  public NewMethodTypeMunger(ResolvedMember signature, Set superMethodsCalled, List typeVariableAliases) {
    super(Method, signature);
    this.typeVariableAliases = typeVariableAliases;
    this.setSuperMethodsCalled(superMethodsCalled);
  }

  public ResolvedMember getInterMethodBody(UnresolvedType aspectType) {
    return AjcMemberMaker.interMethodBody(signature, aspectType);
  }

  /**
   * If the munger has a declared signature
   */
  public ResolvedMember getDeclaredInterMethodBody(UnresolvedType aspectType, World w) {
    if (declaredSignature != null) {
      final ResolvedMember rm = declaredSignature.parameterizedWith(null, signature.getDeclaringType().resolve(w), false,
          getTypeVariableAliases());
      return AjcMemberMaker.interMethodBody(rm, aspectType);
    } else {
      return AjcMemberMaker.interMethodBody(signature, aspectType);
    }
  }

  // public ResolvedMember getInterMethodDispatcher(UnresolvedType aspectType) {
  // return AjcMemberMaker.interMethodDispatcher(signature, aspectType);
  // }

  public ResolvedMember getDeclaredInterMethodDispatcher(UnresolvedType aspectType, World w) {
    if (declaredSignature != null) {
      final ResolvedMember rm = declaredSignature.parameterizedWith(null, signature.getDeclaringType().resolve(w), false,
          getTypeVariableAliases());
      return AjcMemberMaker.interMethodDispatcher(rm, aspectType);
    } else {
      return AjcMemberMaker.interMethodDispatcher(signature, aspectType);
    }
  }

  @Override
  public void write(CompressingDataOutputStream s) throws IOException {
    kind.write(s);
    signature.write(s);
    writeSuperMethodsCalled(s);
    writeSourceLocation(s);
    writeOutTypeAliases(s);
  }

  @Override
  public ResolvedMember getMatchingSyntheticMember(Member member, ResolvedType aspectType) {
    final ResolvedMember ret = AjcMemberMaker.interMethodDispatcher(getSignature(), aspectType);
    if (ResolvedType.matches(ret, member)) {
      return getSignature();
    }
    return super.getMatchingSyntheticMember(member, aspectType);
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
    final NewMethodTypeMunger nmtm = new NewMethodTypeMunger(parameterizedSignature, getSuperMethodsCalled(), typeVariableAliases);
    nmtm.setDeclaredSignature(getSignature());
    nmtm.setSourceLocation(getSourceLocation());
    return nmtm;
  }

  public boolean equals(Object other) {
    if (!(other instanceof NewMethodTypeMunger)) {
      return false;
    }
    final NewMethodTypeMunger o = (NewMethodTypeMunger) other;
    return ((kind == null) ? (o.kind == null) : kind.equals(o.kind))
        && ((signature == null) ? (o.signature == null) : signature.equals(o.signature))
        && ((declaredSignature == null) ? (o.declaredSignature == null) : declaredSignature.equals(o.declaredSignature))
        && ((typeVariableAliases == null) ? (o.typeVariableAliases == null) : typeVariableAliases
        .equals(o.typeVariableAliases));
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + kind.hashCode();
    result = 37 * result + ((signature == null) ? 0 : signature.hashCode());
    result = 37 * result + ((declaredSignature == null) ? 0 : declaredSignature.hashCode());
    result = 37 * result + ((typeVariableAliases == null) ? 0 : typeVariableAliases.hashCode());
    return result;
  }

  @Override
  public ResolvedTypeMunger parameterizeWith(Map<String, UnresolvedType> m, World w) {
    final ResolvedMember parameterizedSignature = getSignature().parameterizedWith(m, w);
    final NewMethodTypeMunger nmtm = new NewMethodTypeMunger(parameterizedSignature, getSuperMethodsCalled(), typeVariableAliases);
    nmtm.setDeclaredSignature(getSignature());
    nmtm.setSourceLocation(getSourceLocation());
    return nmtm;
  }

}
