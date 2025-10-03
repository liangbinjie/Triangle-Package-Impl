package Triangle.AbstractSyntaxTrees;
import Triangle.SyntacticAnalyzer.SourcePosition;

/**
 *
 * @author benji
 */
public class ExportDeclaration extends Declaration {
    public Identifier I;
    
    public ExportDeclaration(Identifier iAST, SourcePosition position) {
        super(position);
        I = iAST;
    }
    
    public Object visit(Visitor v, Object o) {
        return v.visitExportDeclaration(this, o);
    }
}
