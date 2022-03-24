// Generated from /Users/sebastianbode/development/cardano/cf-metadata-server/java/core/src/main/antlr/cddl.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link cddlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface cddlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link cddlParser#cddl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCddl(cddlParser.CddlContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#rule_1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRule_1(cddlParser.Rule_1Context ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#typename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypename(cddlParser.TypenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#groupname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupname(cddlParser.GroupnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#genericparm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericparm(cddlParser.GenericparmContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#genericarg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericarg(cddlParser.GenericargContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(cddlParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#type1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType1(cddlParser.Type1Context ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#type2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType2(cddlParser.Type2Context ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#ctlop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtlop(cddlParser.CtlopContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup(cddlParser.GroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#grpchoice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrpchoice(cddlParser.GrpchoiceContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#grpent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrpent(cddlParser.GrpentContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#memberkey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberkey(cddlParser.MemberkeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#bareword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBareword(cddlParser.BarewordContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#optcom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptcom(cddlParser.OptcomContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#occur}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOccur(cddlParser.OccurContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#uint_1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUint_1(cddlParser.Uint_1Context ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(cddlParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#int_1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt_1(cddlParser.Int_1Context ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(cddlParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#hexfloat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHexfloat(cddlParser.HexfloatContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#fraction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFraction(cddlParser.FractionContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#exponent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponent(cddlParser.ExponentContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#text}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitText(cddlParser.TextContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#schar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchar(cddlParser.ScharContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#bytes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBytes(cddlParser.BytesContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#bchar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBchar(cddlParser.BcharContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(cddlParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#ealpha}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEalpha(cddlParser.EalphaContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#hexdig}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHexdig(cddlParser.HexdigContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#s}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitS(cddlParser.SContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#ws}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWs(cddlParser.WsContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#nl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNl(cddlParser.NlContext ctx);
	/**
	 * Visit a parse tree produced by {@link cddlParser#comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment(cddlParser.CommentContext ctx);
}