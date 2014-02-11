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


import java.util.Iterator;
import java.util.List;

/**
 * Internal AST visitor for serializing an AST in a quick and dirty fashion.
 * For various reasons the resulting string is not necessarily legal
 * Java code; and even if it is legal Java code, it is not necessarily the string
 * that corresponds to the given AST. Although useless for most purposes, it's
 * fine for generating debug print strings.
 * <p>
 * Example usage:
 * <code>
 * <pre>
 *    NaiveASTFlattener p = new NaiveASTFlattener();
 *    node.accept(p);
 *    String result = p.getResult();
 * </pre>
 * </code>
 * Call the <code>reset</code> method to clear the previous result before reusing an
 * existing instance.
 * </p>
 *
 * @since 2.0
 */
public class AjNaiveASTFlattener extends AjASTVisitor {

  /**
   * The string buffer into which the serialized representation of the AST is
   * written.
   */
  protected StringBuffer buffer;

  private int indent = 0;

  /**
   * Creates a new AST printer.
   */
  public AjNaiveASTFlattener() {
    this.buffer = new StringBuffer();
  }

  /**
   * Returns the string accumulated in the visit.
   *
   * @return the serialized
   */
  public String getResult() {
    return this.buffer.toString();
  }

  /**
   * Resets this printer so that it can be used again.
   */
  public void reset() {
    this.buffer.setLength(0);
  }

  void printIndent() {
    for (int i = 0; i < this.indent; i++)
      this.buffer.append("  "); //$NON-NLS-1$
  }

  /**
   * Appends the text representation of the given modifier flags, followed by a single space.
   * Used for 3.0 modifiers and annotations.
   *
   * @param ext the list of modifier and annotation nodes
   *            (element type: <code>IExtendedModifiers</code>)
   */
  void printModifiers(List ext) {
    for (final Iterator it = ext.iterator(); it.hasNext(); ) {
      final ASTNode p = (ASTNode) it.next();
      p.accept(this);
      this.buffer.append(" ");//$NON-NLS-1$
    }
  }

  /**
   * Appends the text representation of the given modifier flags, followed by a single space.
   * Used for JLS2 modifiers.
   *
   * @param modifiers the modifier flags
   */
  void printModifiers(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      this.buffer.append("public ");//$NON-NLS-1$
    }
    if (Modifier.isProtected(modifiers)) {
      this.buffer.append("protected ");//$NON-NLS-1$
    }
    if (Modifier.isPrivate(modifiers)) {
      this.buffer.append("private ");//$NON-NLS-1$
    }
    if (Modifier.isStatic(modifiers)) {
      this.buffer.append("static ");//$NON-NLS-1$
    }
    if (Modifier.isAbstract(modifiers)) {
      this.buffer.append("abstract ");//$NON-NLS-1$
    }
    if (Modifier.isFinal(modifiers)) {
      this.buffer.append("final ");//$NON-NLS-1$
    }
    if (Modifier.isSynchronized(modifiers)) {
      this.buffer.append("synchronized ");//$NON-NLS-1$
    }
    if (Modifier.isVolatile(modifiers)) {
      this.buffer.append("volatile ");//$NON-NLS-1$
    }
    if (Modifier.isNative(modifiers)) {
      this.buffer.append("native ");//$NON-NLS-1$
    }
    if (Modifier.isStrictfp(modifiers)) {
      this.buffer.append("strictfp ");//$NON-NLS-1$
    }
    if (Modifier.isTransient(modifiers)) {
      this.buffer.append("transient ");//$NON-NLS-1$
    }
  }

  /*
   * @see ASTVisitor#visit(AnnotationTypeDeclaration)
   * @since 3.1
   */
  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    printModifiers(node.modifiers());
    this.buffer.append("@interface ");//$NON-NLS-1$
    node.getName().accept(this);
    this.buffer.append(" {");//$NON-NLS-1$
    for (final Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
      final BodyDeclaration d = (BodyDeclaration) it.next();
      d.accept(this);
    }
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(AnnotationTypeMemberDeclaration)
   * @since 3.1
   */
  @Override
  public boolean visit(AnnotationTypeMemberDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    printModifiers(node.modifiers());
    node.getType().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    node.getName().accept(this);
    this.buffer.append("()");//$NON-NLS-1$
    if (node.getDefault() != null) {
      this.buffer.append(" default ");//$NON-NLS-1$
      node.getDefault().accept(this);
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(AnonymousClassDeclaration)
   */
  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    this.buffer.append("{\n");//$NON-NLS-1$
    this.indent++;
    for (final Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
      final BodyDeclaration b = (BodyDeclaration) it.next();
      b.accept(this);
    }
    this.indent--;
    printIndent();
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ArrayAccess)
   */
  @Override
  public boolean visit(ArrayAccess node) {
    node.getArray().accept(this);
    this.buffer.append("[");//$NON-NLS-1$
    node.getIndex().accept(this);
    this.buffer.append("]");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ArrayCreation)
   */
  @Override
  public boolean visit(ArrayCreation node) {
    this.buffer.append("new ");//$NON-NLS-1$
    final ArrayType at = node.getType();
    int dims = at.getDimensions();
    final Type elementType = at.getElementType();
    elementType.accept(this);
    for (final Iterator it = node.dimensions().iterator(); it.hasNext(); ) {
      this.buffer.append("[");//$NON-NLS-1$
      final Expression e = (Expression) it.next();
      e.accept(this);
      this.buffer.append("]");//$NON-NLS-1$
      dims--;
    }
    // add empty "[]" for each extra array dimension
    for (int i = 0; i < dims; i++) {
      this.buffer.append("[]");//$NON-NLS-1$
    }
    if (node.getInitializer() != null) {
      node.getInitializer().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(ArrayInitializer)
   */
  @Override
  public boolean visit(ArrayInitializer node) {
    this.buffer.append("{");//$NON-NLS-1$
    for (final Iterator it = node.expressions().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append("}");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ArrayType)
   */
  @Override
  public boolean visit(ArrayType node) {
    node.getComponentType().accept(this);
    this.buffer.append("[]");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(AssertStatement)
   */
  @Override
  public boolean visit(AssertStatement node) {
    printIndent();
    this.buffer.append("assert ");//$NON-NLS-1$
    node.getExpression().accept(this);
    if (node.getMessage() != null) {
      this.buffer.append(" : ");//$NON-NLS-1$
      node.getMessage().accept(this);
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(Assignment)
   */
  @Override
  public boolean visit(Assignment node) {
    node.getLeftHandSide().accept(this);
    this.buffer.append(node.getOperator().toString());
    node.getRightHandSide().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(Block)
   */
  @Override
  public boolean visit(Block node) {
    this.buffer.append("{\n");//$NON-NLS-1$
    this.indent++;
    for (final Iterator it = node.statements().iterator(); it.hasNext(); ) {
      final Statement s = (Statement) it.next();
      s.accept(this);
    }
    this.indent--;
    printIndent();
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(BlockComment)
   * @since 3.0
   */
  @Override
  public boolean visit(BlockComment node) {
    printIndent();
    this.buffer.append("/* */");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(BooleanLiteral)
   */
  @Override
  public boolean visit(BooleanLiteral node) {
    if (node.booleanValue() == true) {
      this.buffer.append("true");//$NON-NLS-1$
    } else {
      this.buffer.append("false");//$NON-NLS-1$
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(BreakStatement)
   */
  @Override
  public boolean visit(BreakStatement node) {
    printIndent();
    this.buffer.append("break");//$NON-NLS-1$
    if (node.getLabel() != null) {
      this.buffer.append(" ");//$NON-NLS-1$
      node.getLabel().accept(this);
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(CastExpression)
   */
  @Override
  public boolean visit(CastExpression node) {
    this.buffer.append("(");//$NON-NLS-1$
    node.getType().accept(this);
    this.buffer.append(")");//$NON-NLS-1$
    node.getExpression().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(CatchClause)
   */
  @Override
  public boolean visit(CatchClause node) {
    this.buffer.append("catch (");//$NON-NLS-1$
    node.getException().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(CharacterLiteral)
   */
  @Override
  public boolean visit(CharacterLiteral node) {
    this.buffer.append(node.getEscapedValue());
    return false;
  }

  /*
   * @see ASTVisitor#visit(ClassInstanceCreation)
   */
  @Override
  public boolean visit(ClassInstanceCreation node) {
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    this.buffer.append("new ");//$NON-NLS-1$
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      node.internalGetName().accept(this);
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeArguments().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
      node.getType().accept(this);
    }
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    if (node.getAnonymousClassDeclaration() != null) {
      node.getAnonymousClassDeclaration().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(CompilationUnit)
   */
  @Override
  public boolean visit(CompilationUnit node) {
    if (node.getPackage() != null) {
      node.getPackage().accept(this);
    }
    for (final Iterator it = node.imports().iterator(); it.hasNext(); ) {
      final ImportDeclaration d = (ImportDeclaration) it.next();
      d.accept(this);
    }
    for (final Iterator it = node.types().iterator(); it.hasNext(); ) {
      final AbstractTypeDeclaration d = (AbstractTypeDeclaration) it.next();
      d.accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(ConditionalExpression)
   */
  @Override
  public boolean visit(ConditionalExpression node) {
    node.getExpression().accept(this);
    this.buffer.append(" ? ");//$NON-NLS-1$
    node.getThenExpression().accept(this);
    this.buffer.append(" : ");//$NON-NLS-1$
    node.getElseExpression().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(ConstructorInvocation)
   */
  @Override
  public boolean visit(ConstructorInvocation node) {
    printIndent();
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeArguments().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    this.buffer.append("this(");//$NON-NLS-1$
    for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(");\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ContinueStatement)
   */
  @Override
  public boolean visit(ContinueStatement node) {
    printIndent();
    this.buffer.append("continue");//$NON-NLS-1$
    if (node.getLabel() != null) {
      this.buffer.append(" ");//$NON-NLS-1$
      node.getLabel().accept(this);
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(DoStatement)
   */
  @Override
  public boolean visit(DoStatement node) {
    printIndent();
    this.buffer.append("do ");//$NON-NLS-1$
    node.getBody().accept(this);
    this.buffer.append(" while (");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(");\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(EmptyStatement)
   */
  @Override
  public boolean visit(EmptyStatement node) {
    printIndent();
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(EnhancedForStatement)
   * @since 3.1
   */
  @Override
  public boolean visit(EnhancedForStatement node) {
    printIndent();
    this.buffer.append("for (");//$NON-NLS-1$
    node.getParameter().accept(this);
    this.buffer.append(" : ");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  @Override
  public boolean visit(PointcutDeclaration node) {
    printIndent();
    buffer.append(" pointcut ");
    node.getName().accept(this);
    buffer.append("(");
    final List parameters = node.parameters();
    for (final Iterator iter = parameters.iterator(); iter.hasNext(); ) {
      final SingleVariableDeclaration element = (SingleVariableDeclaration) iter.next();
      buffer.append(element.getType().toString() + " " + element.getName());
      if (iter.hasNext())
        buffer.append(", ");
    }
    buffer.append("):");
    node.getDesignator().accept(this);
    buffer.append(";\n");
    return false;
  }

  /*
   * @see ASTVisitor#visit(EnumConstantDeclaration)
   * @since 3.1
   */
  @Override
  public boolean visit(EnumConstantDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    printModifiers(node.modifiers());
    node.getName().accept(this);
    if (!node.arguments().isEmpty()) {
      this.buffer.append("(");//$NON-NLS-1$
      for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
        final Expression e = (Expression) it.next();
        e.accept(this);
        if (it.hasNext()) {
          this.buffer.append(",");//$NON-NLS-1$
        }
      }
      this.buffer.append(")");//$NON-NLS-1$
    }
    if (node.getAnonymousClassDeclaration() != null) {
      node.getAnonymousClassDeclaration().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(EnumDeclaration)
   * @since 3.1
   */
  @Override
  public boolean visit(EnumDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    printModifiers(node.modifiers());
    this.buffer.append("enum ");//$NON-NLS-1$
    node.getName().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    if (!node.superInterfaceTypes().isEmpty()) {
      this.buffer.append("implements ");//$NON-NLS-1$
      for (final Iterator it = node.superInterfaceTypes().iterator(); it.hasNext(); ) {
        final Type t = (Type) it.next();
        t.accept(this);
        if (it.hasNext()) {
          this.buffer.append(", ");//$NON-NLS-1$
        }
      }
      this.buffer.append(" ");//$NON-NLS-1$
    }
    this.buffer.append("{");//$NON-NLS-1$
    for (final Iterator it = node.enumConstants().iterator(); it.hasNext(); ) {
      final EnumConstantDeclaration d = (EnumConstantDeclaration) it.next();
      d.accept(this);
      // enum constant declarations do not include punctuation
      if (it.hasNext()) {
        // enum constant declarations are separated by commas
        this.buffer.append(", ");//$NON-NLS-1$
      }
    }
    if (!node.bodyDeclarations().isEmpty()) {
      this.buffer.append("; ");//$NON-NLS-1$
      for (final Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
        final BodyDeclaration d = (BodyDeclaration) it.next();
        d.accept(this);
        // other body declarations include trailing punctuation
      }
    }
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ExpressionStatement)
   */
  @Override
  public boolean visit(ExpressionStatement node) {
    printIndent();
    node.getExpression().accept(this);
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(FieldAccess)
   */
  @Override
  public boolean visit(FieldAccess node) {
    node.getExpression().accept(this);
    this.buffer.append(".");//$NON-NLS-1$
    node.getName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(FieldDeclaration)
   */
  @Override
  public boolean visit(FieldDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
    node.getType().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    for (final Iterator it = node.fragments().iterator(); it.hasNext(); ) {
      final VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
      f.accept(this);
      if (it.hasNext()) {
        this.buffer.append(", ");//$NON-NLS-1$
      }
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ForStatement)
   */
  @Override
  public boolean visit(ForStatement node) {
    printIndent();
    this.buffer.append("for (");//$NON-NLS-1$
    for (final Iterator it = node.initializers().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) buffer.append(", ");//$NON-NLS-1$
    }
    this.buffer.append("; ");//$NON-NLS-1$
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
    }
    this.buffer.append("; ");//$NON-NLS-1$
    for (final Iterator it = node.updaters().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) buffer.append(", ");//$NON-NLS-1$
    }
    this.buffer.append(") ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(IfStatement)
   */
  @Override
  public boolean visit(IfStatement node) {
    printIndent();
    this.buffer.append("if (");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    node.getThenStatement().accept(this);
    if (node.getElseStatement() != null) {
      this.buffer.append(" else ");//$NON-NLS-1$
      node.getElseStatement().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(ImportDeclaration)
   */
  @Override
  public boolean visit(ImportDeclaration node) {
    printIndent();
    this.buffer.append("import ");//$NON-NLS-1$
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (node.isStatic()) {
        this.buffer.append("static ");//$NON-NLS-1$
      }
    }
    node.getName().accept(this);
    if (node.isOnDemand()) {
      this.buffer.append(".*");//$NON-NLS-1$
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(InfixExpression)
   */
  @Override
  public boolean visit(InfixExpression node) {
    node.getLeftOperand().accept(this);
    this.buffer.append(' ');  // for cases like x= i - -1; or x= i++ + ++i;
    this.buffer.append(node.getOperator().toString());
    this.buffer.append(' ');
    node.getRightOperand().accept(this);
    final List extendedOperands = node.extendedOperands();
    if (extendedOperands.size() != 0) {
      this.buffer.append(' ');
      for (final Iterator it = extendedOperands.iterator(); it.hasNext(); ) {
        this.buffer.append(node.getOperator().toString()).append(' ');
        final Expression e = (Expression) it.next();
        e.accept(this);
      }
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(InstanceofExpression)
   */
  @Override
  public boolean visit(InstanceofExpression node) {
    node.getLeftOperand().accept(this);
    this.buffer.append(" instanceof ");//$NON-NLS-1$
    node.getRightOperand().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(Initializer)
   */
  @Override
  public boolean visit(Initializer node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
    node.getBody().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(Javadoc)
   */
  @Override
  public boolean visit(Javadoc node) {
    printIndent();
    this.buffer.append("/** ");//$NON-NLS-1$
    for (final Iterator it = node.tags().iterator(); it.hasNext(); ) {
      final ASTNode e = (ASTNode) it.next();
      e.accept(this);
    }
    this.buffer.append("\n */\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(LabeledStatement)
   */
  @Override
  public boolean visit(LabeledStatement node) {
    printIndent();
    node.getLabel().accept(this);
    this.buffer.append(": ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(LineComment)
   * @since 3.0
   */
  @Override
  public boolean visit(LineComment node) {
    this.buffer.append("//\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(MarkerAnnotation)
   * @since 3.1
   */
  @Override
  public boolean visit(MarkerAnnotation node) {
    this.buffer.append("@");//$NON-NLS-1$
    node.getTypeName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(MemberRef)
   * @since 3.0
   */
  @Override
  public boolean visit(MemberRef node) {
    if (node.getQualifier() != null) {
      node.getQualifier().accept(this);
    }
    this.buffer.append("#");//$NON-NLS-1$
    node.getName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(MemberValuePair)
   * @since 3.1
   */
  @Override
  public boolean visit(MemberValuePair node) {
    node.getName().accept(this);
    this.buffer.append("=");//$NON-NLS-1$
    node.getValue().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(MethodRef)
   * @since 3.0
   */
  @Override
  public boolean visit(MethodRef node) {
    if (node.getQualifier() != null) {
      node.getQualifier().accept(this);
    }
    this.buffer.append("#");//$NON-NLS-1$
    node.getName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.parameters().iterator(); it.hasNext(); ) {
      final MethodRefParameter e = (MethodRefParameter) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(MethodRefParameter)
   * @since 3.0
   */
  @Override
  public boolean visit(MethodRefParameter node) {
    node.getType().accept(this);
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (node.isVarargs()) {
        this.buffer.append("...");//$NON-NLS-1$
      }
    }
    if (node.getName() != null) {
      this.buffer.append(" ");//$NON-NLS-1$
      node.getName().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(MethodDeclaration)
   */
  @Override
  public boolean visit(MethodDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    printIndent();
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
      if (!node.typeParameters().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeParameters().iterator(); it.hasNext(); ) {
          final TypeParameter t = (TypeParameter) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    if (!node.isConstructor()) {
      if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
        node.internalGetReturnType().accept(this);
      } else {
        if (node.getReturnType2() != null) {
          node.getReturnType2().accept(this);
        } else {
          // methods really ought to have a return type
          this.buffer.append("void");//$NON-NLS-1$
        }
      }
      this.buffer.append(" ");//$NON-NLS-1$
    }
    node.getName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.parameters().iterator(); it.hasNext(); ) {
      final SingleVariableDeclaration v = (SingleVariableDeclaration) it.next();
      v.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    for (int i = 0; i < node.getExtraDimensions(); i++) {
      this.buffer.append("[]"); //$NON-NLS-1$
    }
    if (!node.thrownExceptions().isEmpty()) {
      this.buffer.append(" throws ");//$NON-NLS-1$
      for (final Iterator it = node.thrownExceptions().iterator(); it.hasNext(); ) {
        final Name n = (Name) it.next();
        n.accept(this);
        if (it.hasNext()) {
          this.buffer.append(", ");//$NON-NLS-1$
        }
      }
      this.buffer.append(" ");//$NON-NLS-1$
    }
    if (node.getBody() == null) {
      this.buffer.append(";\n");//$NON-NLS-1$
    } else {
      node.getBody().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(MethodInvocation)
   */
  @Override
  public boolean visit(MethodInvocation node) {
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeArguments().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    node.getName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(Modifier)
   * @since 3.1
   */
  @Override
  public boolean visit(Modifier node) {
    this.buffer.append(node.getKeyword().toString());
    return false;
  }

  /*
   * @see ASTVisitor#visit(NormalAnnotation)
   * @since 3.1
   */
  @Override
  public boolean visit(NormalAnnotation node) {
    this.buffer.append("@");//$NON-NLS-1$
    node.getTypeName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.values().iterator(); it.hasNext(); ) {
      final MemberValuePair p = (MemberValuePair) it.next();
      p.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(NullLiteral)
   */
  @Override
  public boolean visit(NullLiteral node) {
    this.buffer.append("null");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(NumberLiteral)
   */
  @Override
  public boolean visit(NumberLiteral node) {
    this.buffer.append(node.getToken());
    return false;
  }

  /*
   * @see ASTVisitor#visit(PackageDeclaration)
   */
  @Override
  public boolean visit(PackageDeclaration node) {
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (node.getJavadoc() != null) {
        node.getJavadoc().accept(this);
      }
      for (final Iterator it = node.annotations().iterator(); it.hasNext(); ) {
        final Annotation p = (Annotation) it.next();
        p.accept(this);
        this.buffer.append(" ");//$NON-NLS-1$
      }
    }
    printIndent();
    this.buffer.append("package ");//$NON-NLS-1$
    node.getName().accept(this);
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ParameterizedType)
   * @since 3.1
   */
  @Override
  public boolean visit(ParameterizedType node) {
    node.getType().accept(this);
    this.buffer.append("<");//$NON-NLS-1$
    for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
      final Type t = (Type) it.next();
      t.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(">");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ParenthesizedExpression)
   */
  @Override
  public boolean visit(ParenthesizedExpression node) {
    this.buffer.append("(");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(PostfixExpression)
   */
  @Override
  public boolean visit(PostfixExpression node) {
    node.getOperand().accept(this);
    this.buffer.append(node.getOperator().toString());
    return false;
  }

  /*
   * @see ASTVisitor#visit(PrefixExpression)
   */
  @Override
  public boolean visit(PrefixExpression node) {
    this.buffer.append(node.getOperator().toString());
    node.getOperand().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(PrimitiveType)
   */
  @Override
  public boolean visit(PrimitiveType node) {
    this.buffer.append(node.getPrimitiveTypeCode().toString());
    return false;
  }

  /*
   * @see ASTVisitor#visit(QualifiedName)
   */
  @Override
  public boolean visit(QualifiedName node) {
    node.getQualifier().accept(this);
    this.buffer.append(".");//$NON-NLS-1$
    node.getName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(QualifiedType)
   * @since 3.1
   */
  @Override
  public boolean visit(QualifiedType node) {
    node.getQualifier().accept(this);
    this.buffer.append(".");//$NON-NLS-1$
    node.getName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(ReturnStatement)
   */
  @Override
  public boolean visit(ReturnStatement node) {
    printIndent();
    this.buffer.append("return");//$NON-NLS-1$
    if (node.getExpression() != null) {
      this.buffer.append(" ");//$NON-NLS-1$
      node.getExpression().accept(this);
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(SimpleName)
   */
  @Override
  public boolean visit(SimpleName node) {
    this.buffer.append(node.getIdentifier());
    return false;
  }

  /*
   * @see ASTVisitor#visit(SimpleType)
   */
  @Override
  public boolean visit(SimpleType node) {
    return true;
  }

  /*
   * @see ASTVisitor#visit(SingleMemberAnnotation)
   * @since 3.1
   */
  @Override
  public boolean visit(SingleMemberAnnotation node) {
    this.buffer.append("@");//$NON-NLS-1$
    node.getTypeName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    node.getValue().accept(this);
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(SingleVariableDeclaration)
   */
  @Override
  public boolean visit(SingleVariableDeclaration node) {
    printIndent();
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
    node.getType().accept(this);
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (node.isVarargs()) {
        this.buffer.append("...");//$NON-NLS-1$
      }
    }
    this.buffer.append(" ");//$NON-NLS-1$
    node.getName().accept(this);
    for (int i = 0; i < node.getExtraDimensions(); i++) {
      this.buffer.append("[]"); //$NON-NLS-1$
    }
    if (node.getInitializer() != null) {
      this.buffer.append("=");//$NON-NLS-1$
      node.getInitializer().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(StringLiteral)
   */
  @Override
  public boolean visit(StringLiteral node) {
    this.buffer.append(node.getEscapedValue());
    return false;
  }

  /*
   * @see ASTVisitor#visit(SuperConstructorInvocation)
   */
  @Override
  public boolean visit(SuperConstructorInvocation node) {
    printIndent();
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeArguments().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    this.buffer.append("super(");//$NON-NLS-1$
    for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(");\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(SuperFieldAccess)
   */
  @Override
  public boolean visit(SuperFieldAccess node) {
    if (node.getQualifier() != null) {
      node.getQualifier().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    this.buffer.append("super.");//$NON-NLS-1$
    node.getName().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(SuperMethodInvocation)
   */
  @Override
  public boolean visit(SuperMethodInvocation node) {
    if (node.getQualifier() != null) {
      node.getQualifier().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    this.buffer.append("super.");//$NON-NLS-1$
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeArguments().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeArguments().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    node.getName().accept(this);
    this.buffer.append("(");//$NON-NLS-1$
    for (final Iterator it = node.arguments().iterator(); it.hasNext(); ) {
      final Expression e = (Expression) it.next();
      e.accept(this);
      if (it.hasNext()) {
        this.buffer.append(",");//$NON-NLS-1$
      }
    }
    this.buffer.append(")");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(SwitchCase)
   */
  @Override
  public boolean visit(SwitchCase node) {
    if (node.isDefault()) {
      this.buffer.append("default :\n");//$NON-NLS-1$
    } else {
      this.buffer.append("case ");//$NON-NLS-1$
      node.getExpression().accept(this);
      this.buffer.append(":\n");//$NON-NLS-1$
    }
    this.indent++; //decremented in visit(SwitchStatement)
    return false;
  }

  /*
   * @see ASTVisitor#visit(SwitchStatement)
   */
  @Override
  public boolean visit(SwitchStatement node) {
    this.buffer.append("switch (");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    this.buffer.append("{\n");//$NON-NLS-1$
    this.indent++;
    for (final Iterator it = node.statements().iterator(); it.hasNext(); ) {
      final Statement s = (Statement) it.next();
      s.accept(this);
      this.indent--; // incremented in visit(SwitchCase)
    }
    this.indent--;
    printIndent();
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(SynchronizedStatement)
   */
  @Override
  public boolean visit(SynchronizedStatement node) {
    this.buffer.append("synchronized (");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  /*
   * @see ASTVisitor#visit(TagElement)
   * @since 3.0
   */
  @Override
  public boolean visit(TagElement node) {
    if (node.isNested()) {
      // nested tags are always enclosed in braces
      this.buffer.append("{");//$NON-NLS-1$
    } else {
      // top-level tags always begin on a new line
      this.buffer.append("\n * ");//$NON-NLS-1$
    }
    boolean previousRequiresWhiteSpace = false;
    if (node.getTagName() != null) {
      this.buffer.append(node.getTagName());
      previousRequiresWhiteSpace = true;
    }
    boolean previousRequiresNewLine = false;
    for (final Iterator it = node.fragments().iterator(); it.hasNext(); ) {
      final ASTNode e = (ASTNode) it.next();
      // assume text elements include necessary leading and trailing whitespace
      // but Name, MemberRef, MethodRef, and nested TagElement do not include white space
      final boolean currentIncludesWhiteSpace = (e instanceof TextElement);
      if (previousRequiresNewLine && currentIncludesWhiteSpace) {
        this.buffer.append("\n * ");//$NON-NLS-1$
      }
      previousRequiresNewLine = currentIncludesWhiteSpace;
      // add space if required to separate
      if (previousRequiresWhiteSpace && !currentIncludesWhiteSpace) {
        this.buffer.append(" "); //$NON-NLS-1$
      }
      e.accept(this);
      previousRequiresWhiteSpace = !currentIncludesWhiteSpace && !(e instanceof TagElement);
    }
    if (node.isNested()) {
      this.buffer.append("}");//$NON-NLS-1$
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(TextElement)
   * @since 3.0
   */
  @Override
  public boolean visit(TextElement node) {
    this.buffer.append(node.getText());
    return false;
  }

  /*
   * @see ASTVisitor#visit(ThisExpression)
   */
  @Override
  public boolean visit(ThisExpression node) {
    if (node.getQualifier() != null) {
      node.getQualifier().accept(this);
      this.buffer.append(".");//$NON-NLS-1$
    }
    this.buffer.append("this");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(ThrowStatement)
   */
  @Override
  public boolean visit(ThrowStatement node) {
    printIndent();
    this.buffer.append("throw ");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(TryStatement)
   */
  @Override
  public boolean visit(TryStatement node) {
    printIndent();
    this.buffer.append("try ");//$NON-NLS-1$
    node.getBody().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    for (final Iterator it = node.catchClauses().iterator(); it.hasNext(); ) {
      final CatchClause cc = (CatchClause) it.next();
      cc.accept(this);
    }
    if (node.getFinally() != null) {
      this.buffer.append(" finally ");//$NON-NLS-1$
      node.getFinally().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(TypeDeclaration)
   */
  @Override
  public boolean visit(TypeDeclaration node) {
    if (node.getJavadoc() != null) {
      node.getJavadoc().accept(this);
    }
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
//		this.buffer.append(node.isInterface() ? "interface " : "class");//$NON-NLS-2$//$NON-NLS-1$
    if (node.isInterface()) {
      this.buffer.append("interface ");//$NON-NLS-1$
    } else if (((AjTypeDeclaration) node).isAspect()) {
      this.buffer.append("aspect ");//$NON-NLS-1$
    } else {
      this.buffer.append("class ");//$NON-NLS-1$
    }
    node.getName().accept(this);
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (!node.typeParameters().isEmpty()) {
        this.buffer.append("<");//$NON-NLS-1$
        for (final Iterator it = node.typeParameters().iterator(); it.hasNext(); ) {
          final TypeParameter t = (TypeParameter) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(",");//$NON-NLS-1$
          }
        }
        this.buffer.append(">");//$NON-NLS-1$
      }
    }
    this.buffer.append(" ");//$NON-NLS-1$
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      if (node.internalGetSuperclass() != null) {
        this.buffer.append("extends ");//$NON-NLS-1$
        node.internalGetSuperclass().accept(this);
        this.buffer.append(" ");//$NON-NLS-1$
      }
      if (!node.internalSuperInterfaces().isEmpty()) {
//				this.buffer.append(node.isInterface() ? "extends " : "implements ");//$NON-NLS-2$//$NON-NLS-1$
        if (node.isInterface()) {
          this.buffer.append("extends ");//$NON-NLS-1$
        } else {
          this.buffer.append("implements ");//$NON-NLS-1$
        }
        for (final Iterator it = node.internalSuperInterfaces().iterator(); it.hasNext(); ) {
          final Name n = (Name) it.next();
          n.accept(this);
          if (it.hasNext()) {
            this.buffer.append(", ");//$NON-NLS-1$
          }
        }
        this.buffer.append(" ");//$NON-NLS-1$
      }
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      if (node.getSuperclassType() != null) {
        this.buffer.append("extends ");//$NON-NLS-1$
        node.getSuperclassType().accept(this);
        this.buffer.append(" ");//$NON-NLS-1$
      }
      if (!node.superInterfaceTypes().isEmpty()) {
//				this.buffer.append(node.isInterface() ? "extends " : "implements ");//$NON-NLS-2$//$NON-NLS-1$
        if (node.isInterface()) {
          this.buffer.append("extends ");//$NON-NLS-1$
        } else {
          this.buffer.append("implements ");//$NON-NLS-1$
        }
        for (final Iterator it = node.superInterfaceTypes().iterator(); it.hasNext(); ) {
          final Type t = (Type) it.next();
          t.accept(this);
          if (it.hasNext()) {
            this.buffer.append(", ");//$NON-NLS-1$
          }
        }
        this.buffer.append(" ");//$NON-NLS-1$
      }
    }
    this.buffer.append("{\n");//$NON-NLS-1$
    this.indent++;
    final BodyDeclaration prev = null;
    for (final Iterator it = node.bodyDeclarations().iterator(); it.hasNext(); ) {
      final BodyDeclaration d = (BodyDeclaration) it.next();
      if (prev instanceof EnumConstantDeclaration) {
        // enum constant declarations do not include punctuation
        if (d instanceof EnumConstantDeclaration) {
          // enum constant declarations are separated by commas
          this.buffer.append(", ");//$NON-NLS-1$
        } else {
          // semicolon separates last enum constant declaration from
          // first class body declarations
          this.buffer.append("; ");//$NON-NLS-1$
        }
      }
      d.accept(this);
    }
    this.indent--;
    printIndent();
    this.buffer.append("}\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(TypeDeclarationStatement)
   */
  @Override
  public boolean visit(TypeDeclarationStatement node) {
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      node.internalGetTypeDeclaration().accept(this);
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      node.getDeclaration().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(TypeLiteral)
   */
  @Override
  public boolean visit(TypeLiteral node) {
    node.getType().accept(this);
    this.buffer.append(".class");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(TypeParameter)
   * @since 3.1
   */
  @Override
  public boolean visit(TypeParameter node) {
    node.getName().accept(this);
    if (!node.typeBounds().isEmpty()) {
      this.buffer.append(" extends ");//$NON-NLS-1$
      for (final Iterator it = node.typeBounds().iterator(); it.hasNext(); ) {
        final Type t = (Type) it.next();
        t.accept(this);
        if (it.hasNext()) {
          this.buffer.append(" & ");//$NON-NLS-1$
        }
      }
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(VariableDeclarationExpression)
   */
  @Override
  public boolean visit(VariableDeclarationExpression node) {
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
    node.getType().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    for (final Iterator it = node.fragments().iterator(); it.hasNext(); ) {
      final VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
      f.accept(this);
      if (it.hasNext()) {
        this.buffer.append(", ");//$NON-NLS-1$
      }
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(VariableDeclarationFragment)
   */
  @Override
  public boolean visit(VariableDeclarationFragment node) {
    node.getName().accept(this);
    for (int i = 0; i < node.getExtraDimensions(); i++) {
      this.buffer.append("[]");//$NON-NLS-1$
    }
    if (node.getInitializer() != null) {
      this.buffer.append("=");//$NON-NLS-1$
      node.getInitializer().accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(VariableDeclarationStatement)
   */
  @Override
  public boolean visit(VariableDeclarationStatement node) {
    printIndent();
    if (node.getAST().apiLevel() == AST.JLS2_INTERNAL) {
      printModifiers(node.getModifiers());
    }
    if (node.getAST().apiLevel() >= AST.JLS3) {
      printModifiers(node.modifiers());
    }
    node.getType().accept(this);
    this.buffer.append(" ");//$NON-NLS-1$
    for (final Iterator it = node.fragments().iterator(); it.hasNext(); ) {
      final VariableDeclarationFragment f = (VariableDeclarationFragment) it.next();
      f.accept(this);
      if (it.hasNext()) {
        this.buffer.append(", ");//$NON-NLS-1$
      }
    }
    this.buffer.append(";\n");//$NON-NLS-1$
    return false;
  }

  /*
   * @see ASTVisitor#visit(WildcardType)
   * @since 3.1
   */
  @Override
  public boolean visit(WildcardType node) {
    this.buffer.append("?");//$NON-NLS-1$
    final Type bound = node.getBound();
    if (bound != null) {
      if (node.isUpperBound()) {
        this.buffer.append(" extends ");//$NON-NLS-1$
      } else {
        this.buffer.append(" super ");//$NON-NLS-1$
      }
      bound.accept(this);
    }
    return false;
  }

  /*
   * @see ASTVisitor#visit(WhileStatement)
   */
  @Override
  public boolean visit(WhileStatement node) {
    printIndent();
    this.buffer.append("while (");//$NON-NLS-1$
    node.getExpression().accept(this);
    this.buffer.append(") ");//$NON-NLS-1$
    node.getBody().accept(this);
    return false;
  }

  @Override
  public boolean visit(DeclareParentsDeclaration node) {
    printIndent();
    this.buffer.append("declare parents: ");
    node.getChildTypePattern().accept(this);

    if (node.isExtends()) {
      this.buffer.append(" extends ");
    } else {
      this.buffer.append(" implements ");
    }

    for (final Iterator it = node.parentTypePatterns().iterator(); it.hasNext(); ) {
      final TypePattern typePat = (TypePattern) it.next();
      typePat.accept(this);
      if (it.hasNext()) {
        this.buffer.append(", ");
      }
    }

    this.buffer.append(";\n");

    return false;
  }

  @Override
  public boolean visit(DeclareWarningDeclaration node) {
    printIndent();

    this.buffer.append("declare warning: ");
    node.getPointcut().accept(this);
    this.buffer.append(" : ");
    node.getMessage().accept(this);
    this.buffer.append(" ;\n");
    return false;
  }

  @Override
  public boolean visit(DeclareErrorDeclaration node) {
    printIndent();

    this.buffer.append("declare error: ");
    node.getPointcut().accept(this);
    this.buffer.append(" : ");
    node.getMessage().accept(this);
    this.buffer.append(" ;\n");
    return false;
  }

  @Override
  public boolean visit(DeclareSoftDeclaration node) {
    printIndent();

    this.buffer.append("declare soft: ");
    node.getTypePattern().accept(this);
    this.buffer.append(" : ");
    node.getPointcut().accept(this);
    this.buffer.append(" ;\n");
    return false;
  }

  @Override
  public boolean visit(DeclarePrecedenceDeclaration node) {
    printIndent();

    this.buffer.append("declare precedence: ");
    for (final Iterator it = node.typePatterns().iterator(); it.hasNext(); ) {
      final TypePattern typePat = (TypePattern) it.next();
      typePat.accept(this);
      if (it.hasNext()) {
        this.buffer.append(", ");
      }
    }

    this.buffer.append(";\n");

    return false;
  }

  @Override
  public boolean visit(AbstractBooleanTypePattern node) {

    // Flatten boolean expressions in order, meaning
    // the left node needs to be appended first, followed by the
    // boolean operator, followed by the right node.
    node.getLeft().accept(this);
    this.buffer.append(" ");
    this.buffer.append(node.getTypePatternExpression());
    this.buffer.append(" ");
    node.getRight().accept(this);

    // No need to visit the childrena, as they were already visited above
    return false;
  }

  @Override
  public boolean visit(AnyTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(AnyWithAnnotationTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(EllipsisTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(HasMemberTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(IdentifierTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(NotTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    // Visit the children in this case, as the child of a not type pattern
    // is the negated type pattern
    return true;
  }

  @Override
  public boolean visit(NoTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(TypeCategoryTypePattern node) {
    this.buffer.append(node.getTypePatternExpression());
    return false;
  }

  @Override
  public boolean visit(DefaultPointcut node) {
    this.buffer.append(node.getDetail());
    return false;
  }
}
