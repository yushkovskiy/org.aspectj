/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nieraj Singh
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.core.dom;

public class AjASTMatcher extends ASTMatcher {

  /**
   * Creates a new AST matcher instance.
   * <p>
   * For backwards compatibility, the matcher ignores tag elements below doc comments by default. Use {@link #ASTMatcher(boolean)
   * ASTMatcher(true)} for a matcher that compares doc tags by default.
   * </p>
   */
  public AjASTMatcher() {
    this(false);
  }

  /**
   * Creates a new AST matcher instance.
   *
   * @param matchDocTags <code>true</code> if doc comment tags are to be compared by default, and <code>false</code> otherwise
   * @see #match(Javadoc, Object)
   * @since 3.0
   */
  public AjASTMatcher(boolean matchDocTags) {
    super(matchDocTags);
  }

  public boolean match(PointcutDeclaration node, Object other) {
    // ajh02: method added
    if (!(other instanceof PointcutDeclaration)) {
      return false;
    }
    final PointcutDeclaration o = (PointcutDeclaration) other;
    final int level = node.getAST().apiLevel;
    if (level == AST.JLS2_INTERNAL) {
      if (node.getModifiers() != o.getModifiers()) {
        return false;
      }
    }
    if (level >= AST.JLS3) {
      if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
        return false;
      }
    }
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc()) && safeSubtreeMatch(node.getName(), o.getName())
        && safeSubtreeMatch(node.getDesignator(), o.getDesignator());
  }

  public static boolean match(DefaultPointcut node, Object other) {
    if (!(other instanceof DefaultPointcut)) {
      return false;
    }
    return node.getDetail().equals(((DefaultPointcut) other).getDetail());
  }

  public boolean match(ReferencePointcut node, Object other) {
    if (!(other instanceof ReferencePointcut)) {
      return false;
    }
    final ReferencePointcut o = (ReferencePointcut) other;
    // int level = node.getAST().apiLevel;
    return safeSubtreeMatch(node.getName(), o.getName());
    // ajh02: will have to add something here when ReferencePointcuts are given
    // a list of Types for parameters
  }

  public boolean match(NotPointcut node, Object other) {
    if (!(other instanceof NotPointcut)) {
      return false;
    }
    final NotPointcut o = (NotPointcut) other;
    return safeSubtreeMatch(node.getBody(), o.getBody());
  }

  public boolean match(PerObject node, Object other) {
    if (!(other instanceof PerObject)) {
      return false;
    }
    final PerObject o = (PerObject) other;
    return safeSubtreeMatch(node.getBody(), o.getBody()) && o.isThis() == node.isThis();
  }

  public boolean match(PerCflow node, Object other) {
    if (!(other instanceof PerCflow)) {
      return false;
    }
    final PerCflow o = (PerCflow) other;
    return safeSubtreeMatch(node.getBody(), o.getBody()) && node.isBelow() == o.isBelow();
  }

  public static boolean match(PerTypeWithin node, Object other) {
    if (!(other instanceof PerTypeWithin)) {
      return false;
    }
    // PerTypeWithin o = (PerTypeWithin) other;
    return true; // ajh02: stub, should look at the type pattern
  }

  public boolean match(CflowPointcut node, Object other) {
    if (!(other instanceof CflowPointcut)) {
      return false;
    }
    final CflowPointcut o = (CflowPointcut) other;
    return safeSubtreeMatch(node.getBody(), o.getBody());
  }

  public boolean match(AndPointcut node, Object other) {
    if (!(other instanceof AndPointcut)) {
      return false;
    }
    final AndPointcut o = (AndPointcut) other;
    return safeSubtreeMatch(node.getLeft(), o.getLeft()) && safeSubtreeMatch(node.getRight(), o.getRight());
  }

  public boolean match(OrPointcut node, Object other) {
    if (!(other instanceof OrPointcut)) {
      return false;
    }
    final OrPointcut o = (OrPointcut) other;
    return safeSubtreeMatch(node.getLeft(), o.getLeft()) && safeSubtreeMatch(node.getRight(), o.getRight());
  }

  public boolean match(BeforeAdviceDeclaration node, Object other) {
    // ajh02: method added
    if (!(other instanceof BeforeAdviceDeclaration)) {
      return false;
    }
    final BeforeAdviceDeclaration o = (BeforeAdviceDeclaration) other;
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc()) && safeSubtreeListMatch(node.parameters(), o.parameters())
        && safeSubtreeMatch(node.getPointcut(), o.getPointcut())
        && safeSubtreeListMatch(node.thrownExceptions(), o.thrownExceptions())
        && safeSubtreeMatch(node.getBody(), o.getBody());
  }

  public boolean match(AfterAdviceDeclaration node, Object other) {
    // ajh02: todo: should have special methods to match
    // afterReturning and afterThrowing
    if (!(other instanceof AfterAdviceDeclaration)) {
      return false;
    }
    final AfterAdviceDeclaration o = (AfterAdviceDeclaration) other;
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc()) && safeSubtreeListMatch(node.parameters(), o.parameters())
        && safeSubtreeMatch(node.getPointcut(), o.getPointcut())
        && safeSubtreeListMatch(node.thrownExceptions(), o.thrownExceptions())
        && safeSubtreeMatch(node.getBody(), o.getBody());
  }

  public boolean match(AroundAdviceDeclaration node, Object other) {
    if (!(other instanceof AroundAdviceDeclaration)) {
      return false;
    }
    final AroundAdviceDeclaration o = (AroundAdviceDeclaration) other;
    final int level = node.getAST().apiLevel;
    if (level == AST.JLS2_INTERNAL) {
      if (!safeSubtreeMatch(node.internalGetReturnType(), o.internalGetReturnType())) {
        return false;
      }
    }
    if (level >= AST.JLS3) {
      if (!safeSubtreeMatch(node.getReturnType2(), o.getReturnType2())) {
        return false;
      }
      if (!safeSubtreeListMatch(node.typeParameters(), o.typeParameters())) {
        return false;
      }
    }
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc()) && safeSubtreeListMatch(node.parameters(), o.parameters())
        && safeSubtreeMatch(node.getPointcut(), o.getPointcut())
        && safeSubtreeListMatch(node.thrownExceptions(), o.thrownExceptions())
        && safeSubtreeMatch(node.getBody(), o.getBody());
  }

  public boolean match(DeclareDeclaration node, Object other) {
    // ajh02: method added
    if (!(other instanceof DeclareDeclaration)) {
      return false;
    }
    final DeclareDeclaration o = (DeclareDeclaration) other;
    // int level = node.getAST().apiLevel;
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc());
  }

  public boolean match(InterTypeFieldDeclaration node, Object other) {
    // ajh02: method added
    if (!(other instanceof InterTypeFieldDeclaration)) {
      return false;
    }
    final InterTypeFieldDeclaration o = (InterTypeFieldDeclaration) other;
    final int level = node.getAST().apiLevel;
    if (level == AST.JLS2_INTERNAL) {
      if (node.getModifiers() != o.getModifiers()) {
        return false;
      }
    }
    if (level >= AST.JLS3) {
      if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
        return false;
      }
    }
    return safeSubtreeMatch(node.getJavadoc(), o.getJavadoc()) && safeSubtreeMatch(node.getType(), o.getType())
        && safeSubtreeListMatch(node.fragments(), o.fragments());
  }

  public boolean match(InterTypeMethodDeclaration node, Object other) {
    // ajh02: method added
    if (!(other instanceof InterTypeMethodDeclaration)) {
      return false;
    }
    final InterTypeMethodDeclaration o = (InterTypeMethodDeclaration) other;
    final int level = node.getAST().apiLevel;
    if (level == AST.JLS2_INTERNAL) {
      if (node.getModifiers() != o.getModifiers()) {
        return false;
      }
      if (!safeSubtreeMatch(node.internalGetReturnType(), o.internalGetReturnType())) {
        return false;
      }
    }
    if (level >= AST.JLS3) {
      if (!safeSubtreeListMatch(node.modifiers(), o.modifiers())) {
        return false;
      }
      if (!safeSubtreeMatch(node.getReturnType2(), o.getReturnType2())) {
        return false;
      }
      // n.b. compare type parameters even for constructors
      if (!safeSubtreeListMatch(node.typeParameters(), o.typeParameters())) {
        return false;
      }
    }
    return ((node.isConstructor() == o.isConstructor()) && safeSubtreeMatch(node.getJavadoc(), o.getJavadoc())
        && safeSubtreeMatch(node.getName(), o.getName())
        // n.b. compare return type even for constructors
        && safeSubtreeListMatch(node.parameters(), o.parameters()) && node.getExtraDimensions() == o.getExtraDimensions()
        && safeSubtreeListMatch(node.thrownExceptions(), o.thrownExceptions()) && safeSubtreeMatch(node.getBody(), o
        .getBody()));
  }

  public static boolean match(DefaultTypePattern node, Object other) {
    if (!(other instanceof DefaultTypePattern)) {
      return false;
    }
    return node.getDetail().equals(((DefaultTypePattern) other).getDetail());
  }

  public static boolean match(SignaturePattern node, Object other) {
    if (!(other instanceof SignaturePattern)) {
      return false;
    }
    return node.getDetail().equals(((SignaturePattern) other).getDetail());
  }

  public boolean match(AndTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof AndTypePattern)) {
      return false;
    }
    final AndTypePattern otherBoolean = (AndTypePattern) other;
    return safeSubtreeMatch(node.getLeft(), otherBoolean.getLeft())
        && safeSubtreeMatch(node.getRight(), otherBoolean.getRight());
  }

  public boolean match(OrTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof OrTypePattern)) {
      return false;
    }
    final OrTypePattern otherBoolean = (OrTypePattern) other;
    return safeSubtreeMatch(node.getLeft(), otherBoolean.getLeft())
        && safeSubtreeMatch(node.getRight(), otherBoolean.getRight());
  }

  public static boolean match(AnyTypePattern node, Object other) {
    // AnyTypePattern nodes don't hold state aside from the AST, so just do a reference check
    if (node == other) {
      return true;
    }
    return false;
  }

  public static boolean match(AnyWithAnnotationTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof AnyWithAnnotationTypePattern)) {
      return false;
    }
    // For now only do an expression matching. In future versions, when
    // the node supports AnnotationTypes, this may have to be changed
    return node.getTypePatternExpression().equals(
        ((AnyWithAnnotationTypePattern) other)
            .getTypePatternExpression());
  }

  public static boolean match(EllipsisTypePattern node, Object other) {
    // Ellipsis nodes don't hold state aside from the AST, so just do a reference check
    if (node == other) {
      return true;
    }
    return false;
  }

  public boolean match(NotTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof NotTypePattern)) {
      return false;
    }
    return safeSubtreeMatch(node.getNegatedTypePattern(),
        ((NotTypePattern) other).getNegatedTypePattern());
  }

  public static boolean match(NoTypePattern node, Object other) {
    // NoTypePattern nodes don't hold state aside from the AST, so just do a reference check
    if (node == other) {
      return true;
    }
    return false;
  }

  public boolean match(HasMemberTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof HasMemberTypePattern)) {
      return false;
    }
    return safeSubtreeMatch(node.getSignaturePattern(),
        ((HasMemberTypePattern) other).getSignaturePattern());
  }

  public boolean match(IdentifierTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof IdentifierTypePattern)) {
      return false;
    }
    return safeSubtreeMatch(node.getType(),
        ((IdentifierTypePattern) other).getType());
  }

  public static boolean match(TypeCategoryTypePattern node, Object other) {
    if (node == other) {
      return true;
    }
    if (!(other instanceof TypeCategoryTypePattern)) {
      return false;
    }
    return node.getTypeCategory() == ((TypeCategoryTypePattern) other)
        .getTypeCategory();
  }

  public static boolean match(Type type, Object other) {
    if (type == other) {
      return true;
    }
    // For now only support simple type/simple name matching. Support for
    // other types
    // may have to be added here
    if (type instanceof SimpleType && other instanceof SimpleType) {
      final Name name = ((SimpleType) type).getName();
      final Name otherName = ((SimpleType) other).getName();
      if (name instanceof SimpleName && otherName instanceof SimpleName) {
        return ((SimpleName) name).getIdentifier().equals(
            ((SimpleName) otherName).getIdentifier());
      }
    }
    return false;
  }
}
