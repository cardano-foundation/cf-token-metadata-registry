// Generated from /Users/sebastianbode/development/cardano/cf-metadata-server/java/core/src/main/antlr/cddl.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link cddlParser}.
 */
public interface cddlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link cddlParser#cddl}.
	 * @param ctx the parse tree
	 */
	void enterCddl(cddlParser.CddlContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#cddl}.
	 * @param ctx the parse tree
	 */
	void exitCddl(cddlParser.CddlContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#rule_1}.
	 * @param ctx the parse tree
	 */
	void enterRule_1(cddlParser.Rule_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#rule_1}.
	 * @param ctx the parse tree
	 */
	void exitRule_1(cddlParser.Rule_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#typename}.
	 * @param ctx the parse tree
	 */
	void enterTypename(cddlParser.TypenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#typename}.
	 * @param ctx the parse tree
	 */
	void exitTypename(cddlParser.TypenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#groupname}.
	 * @param ctx the parse tree
	 */
	void enterGroupname(cddlParser.GroupnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#groupname}.
	 * @param ctx the parse tree
	 */
	void exitGroupname(cddlParser.GroupnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#genericparm}.
	 * @param ctx the parse tree
	 */
	void enterGenericparm(cddlParser.GenericparmContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#genericparm}.
	 * @param ctx the parse tree
	 */
	void exitGenericparm(cddlParser.GenericparmContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#genericarg}.
	 * @param ctx the parse tree
	 */
	void enterGenericarg(cddlParser.GenericargContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#genericarg}.
	 * @param ctx the parse tree
	 */
	void exitGenericarg(cddlParser.GenericargContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(cddlParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(cddlParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#type1}.
	 * @param ctx the parse tree
	 */
	void enterType1(cddlParser.Type1Context ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#type1}.
	 * @param ctx the parse tree
	 */
	void exitType1(cddlParser.Type1Context ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#type2}.
	 * @param ctx the parse tree
	 */
	void enterType2(cddlParser.Type2Context ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#type2}.
	 * @param ctx the parse tree
	 */
	void exitType2(cddlParser.Type2Context ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#ctlop}.
	 * @param ctx the parse tree
	 */
	void enterCtlop(cddlParser.CtlopContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#ctlop}.
	 * @param ctx the parse tree
	 */
	void exitCtlop(cddlParser.CtlopContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#group}.
	 * @param ctx the parse tree
	 */
	void enterGroup(cddlParser.GroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#group}.
	 * @param ctx the parse tree
	 */
	void exitGroup(cddlParser.GroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#grpchoice}.
	 * @param ctx the parse tree
	 */
	void enterGrpchoice(cddlParser.GrpchoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#grpchoice}.
	 * @param ctx the parse tree
	 */
	void exitGrpchoice(cddlParser.GrpchoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#grpent}.
	 * @param ctx the parse tree
	 */
	void enterGrpent(cddlParser.GrpentContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#grpent}.
	 * @param ctx the parse tree
	 */
	void exitGrpent(cddlParser.GrpentContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#memberkey}.
	 * @param ctx the parse tree
	 */
	void enterMemberkey(cddlParser.MemberkeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#memberkey}.
	 * @param ctx the parse tree
	 */
	void exitMemberkey(cddlParser.MemberkeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#bareword}.
	 * @param ctx the parse tree
	 */
	void enterBareword(cddlParser.BarewordContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#bareword}.
	 * @param ctx the parse tree
	 */
	void exitBareword(cddlParser.BarewordContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#optcom}.
	 * @param ctx the parse tree
	 */
	void enterOptcom(cddlParser.OptcomContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#optcom}.
	 * @param ctx the parse tree
	 */
	void exitOptcom(cddlParser.OptcomContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#occur}.
	 * @param ctx the parse tree
	 */
	void enterOccur(cddlParser.OccurContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#occur}.
	 * @param ctx the parse tree
	 */
	void exitOccur(cddlParser.OccurContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#uint_1}.
	 * @param ctx the parse tree
	 */
	void enterUint_1(cddlParser.Uint_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#uint_1}.
	 * @param ctx the parse tree
	 */
	void exitUint_1(cddlParser.Uint_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(cddlParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(cddlParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#int_1}.
	 * @param ctx the parse tree
	 */
	void enterInt_1(cddlParser.Int_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#int_1}.
	 * @param ctx the parse tree
	 */
	void exitInt_1(cddlParser.Int_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(cddlParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(cddlParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#hexfloat}.
	 * @param ctx the parse tree
	 */
	void enterHexfloat(cddlParser.HexfloatContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#hexfloat}.
	 * @param ctx the parse tree
	 */
	void exitHexfloat(cddlParser.HexfloatContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#fraction}.
	 * @param ctx the parse tree
	 */
	void enterFraction(cddlParser.FractionContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#fraction}.
	 * @param ctx the parse tree
	 */
	void exitFraction(cddlParser.FractionContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#exponent}.
	 * @param ctx the parse tree
	 */
	void enterExponent(cddlParser.ExponentContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#exponent}.
	 * @param ctx the parse tree
	 */
	void exitExponent(cddlParser.ExponentContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#text}.
	 * @param ctx the parse tree
	 */
	void enterText(cddlParser.TextContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#text}.
	 * @param ctx the parse tree
	 */
	void exitText(cddlParser.TextContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#schar}.
	 * @param ctx the parse tree
	 */
	void enterSchar(cddlParser.ScharContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#schar}.
	 * @param ctx the parse tree
	 */
	void exitSchar(cddlParser.ScharContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#bytes}.
	 * @param ctx the parse tree
	 */
	void enterBytes(cddlParser.BytesContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#bytes}.
	 * @param ctx the parse tree
	 */
	void exitBytes(cddlParser.BytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#bchar}.
	 * @param ctx the parse tree
	 */
	void enterBchar(cddlParser.BcharContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#bchar}.
	 * @param ctx the parse tree
	 */
	void exitBchar(cddlParser.BcharContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(cddlParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(cddlParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#ealpha}.
	 * @param ctx the parse tree
	 */
	void enterEalpha(cddlParser.EalphaContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#ealpha}.
	 * @param ctx the parse tree
	 */
	void exitEalpha(cddlParser.EalphaContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#hexdig}.
	 * @param ctx the parse tree
	 */
	void enterHexdig(cddlParser.HexdigContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#hexdig}.
	 * @param ctx the parse tree
	 */
	void exitHexdig(cddlParser.HexdigContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#s}.
	 * @param ctx the parse tree
	 */
	void enterS(cddlParser.SContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#s}.
	 * @param ctx the parse tree
	 */
	void exitS(cddlParser.SContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#ws}.
	 * @param ctx the parse tree
	 */
	void enterWs(cddlParser.WsContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#ws}.
	 * @param ctx the parse tree
	 */
	void exitWs(cddlParser.WsContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#nl}.
	 * @param ctx the parse tree
	 */
	void enterNl(cddlParser.NlContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#nl}.
	 * @param ctx the parse tree
	 */
	void exitNl(cddlParser.NlContext ctx);
	/**
	 * Enter a parse tree produced by {@link cddlParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(cddlParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link cddlParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(cddlParser.CommentContext ctx);
}