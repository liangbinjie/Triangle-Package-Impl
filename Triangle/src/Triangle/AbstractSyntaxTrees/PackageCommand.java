package Triangle.AbstractSyntaxTrees;
import Triangle.SyntacticAnalyzer.SourcePosition;

/**
 *
 * @author benji
 */
public class PackageCommand extends Command {
    public PackageCommand (Identifier iAST, Declaration dAST, SourcePosition thePosition) {
        super (thePosition);
        I = iAST;
        D = dAST;
    }

    public Object visit(Visitor v, Object o) {
        return v.visitPackageCommand(this, o);
    }

    public Identifier I;
    public Declaration D;
}
