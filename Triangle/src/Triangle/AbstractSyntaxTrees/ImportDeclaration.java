package Triangle.AbstractSyntaxTrees;

import Triangle.SyntacticAnalyzer.SourcePosition;

public class ImportDeclaration extends Declaration {
  // Paquete a importar, p.ej. Utils
  public Identifier packageId;
  
  // Nombres específicos si venía "from P import a, b"; null si era "import P"
  public final Identifier[] names;

  public ImportDeclaration(Identifier packageId, Identifier[] names, SourcePosition pos) {
    super(pos);
    this.packageId = packageId;
    this.names = names; // null => import completo (acceso calificado)
  }

  @Override
  public Object visit(Visitor v, Object o) {
    return v.visitImportDeclaration(this, o);
  }
}
