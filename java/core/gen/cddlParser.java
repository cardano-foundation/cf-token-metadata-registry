// Generated from /Users/sebastianbode/development/cardano/cf-metadata-server/java/core/src/main/antlr/cddl.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class cddlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		ASSIGNT=46, ASSIGNG=47, RANGEOP=48, SCHAR=49, SESC=50, BCHAR=51, BSQUAL=52, 
		ALPHA=53, DIGIT=54, DIGIT1=55, BINDIG=56, SP=57, PCHAR=58, CRLF=59;
	public static final int
		RULE_cddl = 0, RULE_rule_1 = 1, RULE_typename = 2, RULE_groupname = 3, 
		RULE_genericparm = 4, RULE_genericarg = 5, RULE_type = 6, RULE_type1 = 7, 
		RULE_type2 = 8, RULE_ctlop = 9, RULE_group = 10, RULE_grpchoice = 11, 
		RULE_grpent = 12, RULE_memberkey = 13, RULE_bareword = 14, RULE_optcom = 15, 
		RULE_occur = 16, RULE_uint_1 = 17, RULE_value = 18, RULE_int_1 = 19, RULE_number = 20, 
		RULE_hexfloat = 21, RULE_fraction = 22, RULE_exponent = 23, RULE_text = 24, 
		RULE_schar = 25, RULE_bytes = 26, RULE_bchar = 27, RULE_id = 28, RULE_ealpha = 29, 
		RULE_hexdig = 30, RULE_s = 31, RULE_ws = 32, RULE_nl = 33, RULE_comment = 34;
	private static String[] makeRuleNames() {
		return new String[] {
			"cddl", "rule_1", "typename", "groupname", "genericparm", "genericarg", 
			"type", "type1", "type2", "ctlop", "group", "grpchoice", "grpent", "memberkey", 
			"bareword", "optcom", "occur", "uint_1", "value", "int_1", "number", 
			"hexfloat", "fraction", "exponent", "text", "schar", "bytes", "bchar", 
			"id", "ealpha", "hexdig", "s", "ws", "nl", "comment"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<'", "','", "'>'", "'/'", "'('", "')'", "'{'", "'}'", "'['", 
			"']'", "'~'", "'&'", "'#'", "'6'", "'.'", "'^'", "'='", "':'", "'*'", 
			"'+'", "'?'", "'0'", "'X'", "'x'", "'B'", "'b'", "'-'", "'E'", "'e'", 
			"'P'", "'p'", "'\u0022'", "'\u0027'", "'@'", "'_'", "'$'", "'A'", "'a'", 
			"'C'", "'c'", "'D'", "'d'", "'F'", "'f'", "';'", null, null, null, null, 
			null, null, null, null, null, null, null, "'\u0020'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, "ASSIGNT", 
			"ASSIGNG", "RANGEOP", "SCHAR", "SESC", "BCHAR", "BSQUAL", "ALPHA", "DIGIT", 
			"DIGIT1", "BINDIG", "SP", "PCHAR", "CRLF"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "cddl.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public cddlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class CddlContext extends ParserRuleContext {
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public List<Rule_1Context> rule_1() {
			return getRuleContexts(Rule_1Context.class);
		}
		public Rule_1Context rule_1(int i) {
			return getRuleContext(Rule_1Context.class,i);
		}
		public CddlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cddl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterCddl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitCddl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitCddl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CddlContext cddl() throws RecognitionException {
		CddlContext _localctx = new CddlContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_cddl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			s();
			setState(74); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(71);
				rule_1();
				setState(72);
				s();
				}
				}
				setState(76); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << ALPHA))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Rule_1Context extends ParserRuleContext {
		public TypenameContext typename() {
			return getRuleContext(TypenameContext.class,0);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public TerminalNode ASSIGNT() { return getToken(cddlParser.ASSIGNT, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public GenericparmContext genericparm() {
			return getRuleContext(GenericparmContext.class,0);
		}
		public GroupnameContext groupname() {
			return getRuleContext(GroupnameContext.class,0);
		}
		public TerminalNode ASSIGNG() { return getToken(cddlParser.ASSIGNG, 0); }
		public GrpentContext grpent() {
			return getRuleContext(GrpentContext.class,0);
		}
		public Rule_1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rule_1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterRule_1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitRule_1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitRule_1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Rule_1Context rule_1() throws RecognitionException {
		Rule_1Context _localctx = new Rule_1Context(_ctx, getState());
		enterRule(_localctx, 2, RULE_rule_1);
		int _la;
		try {
			setState(96);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(78);
				typename();
				setState(80);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(79);
					genericparm();
					}
				}

				setState(82);
				s();
				setState(83);
				match(ASSIGNT);
				setState(84);
				s();
				setState(85);
				type();
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(87);
				groupname();
				setState(89);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(88);
					genericparm();
					}
				}

				setState(91);
				s();
				setState(92);
				match(ASSIGNG);
				setState(93);
				s();
				setState(94);
				grpent();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypenameContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypenameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typename; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterTypename(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitTypename(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitTypename(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypenameContext typename() throws RecognitionException {
		TypenameContext _localctx = new TypenameContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_typename);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(98);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupnameContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public GroupnameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupname; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGroupname(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGroupname(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGroupname(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupnameContext groupname() throws RecognitionException {
		GroupnameContext _localctx = new GroupnameContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_groupname);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericparmContext extends ParserRuleContext {
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public GenericparmContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericparm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGenericparm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGenericparm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGenericparm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericparmContext genericparm() throws RecognitionException {
		GenericparmContext _localctx = new GenericparmContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_genericparm);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(T__0);
			setState(103);
			s();
			setState(104);
			id();
			setState(105);
			s();
			setState(113);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(106);
				match(T__1);
				setState(107);
				s();
				setState(108);
				id();
				setState(109);
				s();
				}
				}
				setState(115);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(116);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericargContext extends ParserRuleContext {
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public List<Type1Context> type1() {
			return getRuleContexts(Type1Context.class);
		}
		public Type1Context type1(int i) {
			return getRuleContext(Type1Context.class,i);
		}
		public GenericargContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericarg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGenericarg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGenericarg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGenericarg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericargContext genericarg() throws RecognitionException {
		GenericargContext _localctx = new GenericargContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_genericarg);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			match(T__0);
			setState(119);
			s();
			setState(120);
			type1();
			setState(121);
			s();
			setState(129);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(122);
				match(T__1);
				setState(123);
				s();
				setState(124);
				type1();
				setState(125);
				s();
				}
				}
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(132);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public List<Type1Context> type1() {
			return getRuleContexts(Type1Context.class);
		}
		public Type1Context type1(int i) {
			return getRuleContext(Type1Context.class,i);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_type);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(134);
			type1();
			setState(142);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(135);
					s();
					setState(136);
					match(T__3);
					setState(137);
					s();
					setState(138);
					type1();
					}
					} 
				}
				setState(144);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type1Context extends ParserRuleContext {
		public List<Type2Context> type2() {
			return getRuleContexts(Type2Context.class);
		}
		public Type2Context type2(int i) {
			return getRuleContext(Type2Context.class,i);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public TerminalNode RANGEOP() { return getToken(cddlParser.RANGEOP, 0); }
		public CtlopContext ctlop() {
			return getRuleContext(CtlopContext.class,0);
		}
		public Type1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterType1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitType1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitType1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type1Context type1() throws RecognitionException {
		Type1Context _localctx = new Type1Context(_ctx, getState());
		enterRule(_localctx, 14, RULE_type1);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			type2();
			setState(154);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(146);
				s();
				setState(149);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case RANGEOP:
					{
					setState(147);
					match(RANGEOP);
					}
					break;
				case T__14:
					{
					setState(148);
					ctlop();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(151);
				s();
				setState(152);
				type2();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type2Context extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public TypenameContext typename() {
			return getRuleContext(TypenameContext.class,0);
		}
		public GenericargContext genericarg() {
			return getRuleContext(GenericargContext.class,0);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public GroupContext group() {
			return getRuleContext(GroupContext.class,0);
		}
		public GroupnameContext groupname() {
			return getRuleContext(GroupnameContext.class,0);
		}
		public Uint_1Context uint_1() {
			return getRuleContext(Uint_1Context.class,0);
		}
		public TerminalNode DIGIT() { return getToken(cddlParser.DIGIT, 0); }
		public Type2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type2; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterType2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitType2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitType2(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type2Context type2() throws RecognitionException {
		Type2Context _localctx = new Type2Context(_ctx, getState());
		enterRule(_localctx, 16, RULE_type2);
		int _la;
		try {
			setState(218);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(156);
				value();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(157);
				typename();
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(158);
					genericarg();
					}
				}

				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(161);
				match(T__4);
				setState(162);
				s();
				setState(163);
				type();
				setState(164);
				s();
				setState(165);
				match(T__5);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				{
				setState(167);
				match(T__6);
				setState(168);
				s();
				setState(169);
				group();
				setState(170);
				s();
				setState(171);
				match(T__7);
				}
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				{
				setState(173);
				match(T__8);
				setState(174);
				s();
				setState(175);
				group();
				setState(176);
				s();
				setState(177);
				match(T__9);
				}
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				{
				setState(179);
				match(T__10);
				setState(180);
				s();
				setState(181);
				typename();
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(182);
					genericarg();
					}
				}

				}
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				{
				setState(185);
				match(T__11);
				setState(186);
				s();
				setState(187);
				match(T__4);
				setState(188);
				s();
				setState(189);
				group();
				setState(190);
				s();
				setState(191);
				match(T__5);
				}
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				{
				setState(193);
				match(T__11);
				setState(194);
				s();
				setState(195);
				groupname();
				setState(197);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(196);
					genericarg();
					}
				}

				}
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				{
				setState(199);
				match(T__12);
				setState(200);
				match(T__13);
				setState(203);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__14) {
					{
					setState(201);
					match(T__14);
					setState(202);
					uint_1();
					}
				}

				setState(205);
				match(T__4);
				setState(206);
				s();
				setState(207);
				type();
				setState(208);
				s();
				setState(209);
				match(T__5);
				}
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				{
				setState(211);
				match(T__12);
				setState(212);
				match(DIGIT);
				setState(215);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(213);
					match(T__14);
					setState(214);
					uint_1();
					}
					break;
				}
				}
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(217);
				match(T__12);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CtlopContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public CtlopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ctlop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterCtlop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitCtlop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitCtlop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CtlopContext ctlop() throws RecognitionException {
		CtlopContext _localctx = new CtlopContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_ctlop);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(220);
			match(T__14);
			setState(221);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupContext extends ParserRuleContext {
		public List<GrpchoiceContext> grpchoice() {
			return getRuleContexts(GrpchoiceContext.class);
		}
		public GrpchoiceContext grpchoice(int i) {
			return getRuleContext(GrpchoiceContext.class,i);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public GroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_group; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupContext group() throws RecognitionException {
		GroupContext _localctx = new GroupContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_group);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(223);
			grpchoice();
			setState(233);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(224);
					s();
					{
					setState(225);
					match(T__3);
					setState(226);
					match(T__3);
					}
					setState(228);
					s();
					setState(229);
					grpchoice();
					}
					} 
				}
				setState(235);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GrpchoiceContext extends ParserRuleContext {
		public List<GrpentContext> grpent() {
			return getRuleContexts(GrpentContext.class);
		}
		public GrpentContext grpent(int i) {
			return getRuleContext(GrpentContext.class,i);
		}
		public List<OptcomContext> optcom() {
			return getRuleContexts(OptcomContext.class);
		}
		public OptcomContext optcom(int i) {
			return getRuleContext(OptcomContext.class,i);
		}
		public GrpchoiceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grpchoice; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGrpchoice(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGrpchoice(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGrpchoice(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GrpchoiceContext grpchoice() throws RecognitionException {
		GrpchoiceContext _localctx = new GrpchoiceContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_grpchoice);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__4) | (1L << T__6) | (1L << T__8) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__26) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << BSQUAL) | (1L << ALPHA) | (1L << DIGIT1))) != 0)) {
				{
				{
				setState(236);
				grpent();
				setState(237);
				optcom();
				}
				}
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GrpentContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public OccurContext occur() {
			return getRuleContext(OccurContext.class,0);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public MemberkeyContext memberkey() {
			return getRuleContext(MemberkeyContext.class,0);
		}
		public GroupnameContext groupname() {
			return getRuleContext(GroupnameContext.class,0);
		}
		public GenericargContext genericarg() {
			return getRuleContext(GenericargContext.class,0);
		}
		public GroupContext group() {
			return getRuleContext(GroupContext.class,0);
		}
		public GrpentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grpent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterGrpent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitGrpent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitGrpent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GrpentContext grpent() throws RecognitionException {
		GrpentContext _localctx = new GrpentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_grpent);
		int _la;
		try {
			setState(275);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(247);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(244);
					occur();
					setState(245);
					s();
					}
					break;
				}
				setState(252);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
				case 1:
					{
					setState(249);
					memberkey();
					setState(250);
					s();
					}
					break;
				}
				setState(254);
				type();
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(258);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << DIGIT1))) != 0)) {
					{
					setState(255);
					occur();
					setState(256);
					s();
					}
				}

				setState(260);
				groupname();
				setState(262);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(261);
					genericarg();
					}
				}

				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(267);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << DIGIT1))) != 0)) {
					{
					setState(264);
					occur();
					setState(265);
					s();
					}
				}

				setState(269);
				match(T__4);
				setState(270);
				s();
				setState(271);
				group();
				setState(272);
				s();
				setState(273);
				match(T__5);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MemberkeyContext extends ParserRuleContext {
		public Type1Context type1() {
			return getRuleContext(Type1Context.class,0);
		}
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public BarewordContext bareword() {
			return getRuleContext(BarewordContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public MemberkeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_memberkey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterMemberkey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitMemberkey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitMemberkey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MemberkeyContext memberkey() throws RecognitionException {
		MemberkeyContext _localctx = new MemberkeyContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_memberkey);
		int _la;
		try {
			setState(294);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(277);
				type1();
				setState(278);
				s();
				setState(281);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__15) {
					{
					setState(279);
					match(T__15);
					setState(280);
					s();
					}
				}

				{
				setState(283);
				match(T__16);
				setState(284);
				match(T__2);
				}
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(286);
				bareword();
				setState(287);
				s();
				setState(288);
				match(T__17);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(290);
				value();
				setState(291);
				s();
				setState(292);
				match(T__17);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BarewordContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public BarewordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bareword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterBareword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitBareword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitBareword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BarewordContext bareword() throws RecognitionException {
		BarewordContext _localctx = new BarewordContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_bareword);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OptcomContext extends ParserRuleContext {
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
		public OptcomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optcom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterOptcom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitOptcom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitOptcom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptcomContext optcom() throws RecognitionException {
		OptcomContext _localctx = new OptcomContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_optcom);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			s();
			setState(301);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(299);
				match(T__1);
				setState(300);
				s();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OccurContext extends ParserRuleContext {
		public List<Uint_1Context> uint_1() {
			return getRuleContexts(Uint_1Context.class);
		}
		public Uint_1Context uint_1(int i) {
			return getRuleContext(Uint_1Context.class,i);
		}
		public OccurContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_occur; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterOccur(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitOccur(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitOccur(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OccurContext occur() throws RecognitionException {
		OccurContext _localctx = new OccurContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_occur);
		int _la;
		try {
			setState(312);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__18:
			case T__21:
			case DIGIT1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(304);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__21 || _la==DIGIT1) {
					{
					setState(303);
					uint_1();
					}
				}

				setState(306);
				match(T__18);
				setState(308);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(307);
					uint_1();
					}
					break;
				}
				}
				}
				break;
			case T__19:
				enterOuterAlt(_localctx, 2);
				{
				setState(310);
				match(T__19);
				}
				break;
			case T__20:
				enterOuterAlt(_localctx, 3);
				{
				setState(311);
				match(T__20);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Uint_1Context extends ParserRuleContext {
		public TerminalNode DIGIT1() { return getToken(cddlParser.DIGIT1, 0); }
		public List<TerminalNode> DIGIT() { return getTokens(cddlParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(cddlParser.DIGIT, i);
		}
		public List<HexdigContext> hexdig() {
			return getRuleContexts(HexdigContext.class);
		}
		public HexdigContext hexdig(int i) {
			return getRuleContext(HexdigContext.class,i);
		}
		public List<TerminalNode> BINDIG() { return getTokens(cddlParser.BINDIG); }
		public TerminalNode BINDIG(int i) {
			return getToken(cddlParser.BINDIG, i);
		}
		public Uint_1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_uint_1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterUint_1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitUint_1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitUint_1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Uint_1Context uint_1() throws RecognitionException {
		Uint_1Context _localctx = new Uint_1Context(_ctx, getState());
		enterRule(_localctx, 34, RULE_uint_1);
		int _la;
		try {
			int _alt;
			setState(338);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(314);
				match(DIGIT1);
				setState(318);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DIGIT) {
					{
					{
					setState(315);
					match(DIGIT);
					}
					}
					setState(320);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				{
				setState(321);
				match(T__21);
				setState(322);
				_la = _input.LA(1);
				if ( !(_la==T__22 || _la==T__23) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				setState(325); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(324);
						hexdig();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(327); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				{
				setState(329);
				match(T__21);
				setState(330);
				_la = _input.LA(1);
				if ( !(_la==T__24 || _la==T__25) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				setState(333); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(332);
					match(BINDIG);
					}
					}
					setState(335); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==BINDIG );
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(337);
				match(T__21);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public BytesContext bytes() {
			return getRuleContext(BytesContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_value);
		try {
			setState(343);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__21:
			case T__26:
			case DIGIT1:
				enterOuterAlt(_localctx, 1);
				{
				setState(340);
				number();
				}
				break;
			case T__31:
				enterOuterAlt(_localctx, 2);
				{
				setState(341);
				text();
				}
				break;
			case T__32:
			case BSQUAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(342);
				bytes();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Int_1Context extends ParserRuleContext {
		public Uint_1Context uint_1() {
			return getRuleContext(Uint_1Context.class,0);
		}
		public Int_1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_int_1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterInt_1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitInt_1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitInt_1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Int_1Context int_1() throws RecognitionException {
		Int_1Context _localctx = new Int_1Context(_ctx, getState());
		enterRule(_localctx, 38, RULE_int_1);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__26) {
				{
				setState(345);
				match(T__26);
				}
			}

			setState(348);
			uint_1();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public HexfloatContext hexfloat() {
			return getRuleContext(HexfloatContext.class,0);
		}
		public Int_1Context int_1() {
			return getRuleContext(Int_1Context.class,0);
		}
		public FractionContext fraction() {
			return getRuleContext(FractionContext.class,0);
		}
		public ExponentContext exponent() {
			return getRuleContext(ExponentContext.class,0);
		}
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_number);
		int _la;
		try {
			setState(360);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(350);
				hexfloat();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(351);
				int_1();
				setState(354);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
				case 1:
					{
					setState(352);
					match(T__14);
					setState(353);
					fraction();
					}
					break;
				}
				setState(358);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__27 || _la==T__28) {
					{
					setState(356);
					_la = _input.LA(1);
					if ( !(_la==T__27 || _la==T__28) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(357);
					exponent();
					}
				}

				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HexfloatContext extends ParserRuleContext {
		public ExponentContext exponent() {
			return getRuleContext(ExponentContext.class,0);
		}
		public List<HexdigContext> hexdig() {
			return getRuleContexts(HexdigContext.class);
		}
		public HexdigContext hexdig(int i) {
			return getRuleContext(HexdigContext.class,i);
		}
		public HexfloatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hexfloat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterHexfloat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitHexfloat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitHexfloat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HexfloatContext hexfloat() throws RecognitionException {
		HexfloatContext _localctx = new HexfloatContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_hexfloat);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(363);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__26) {
				{
				setState(362);
				match(T__26);
				}
			}

			{
			setState(365);
			match(T__21);
			setState(366);
			_la = _input.LA(1);
			if ( !(_la==T__22 || _la==T__23) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
			setState(369); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(368);
				hexdig();
				}
				}
				setState(371); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__24) | (1L << T__25) | (1L << T__27) | (1L << T__28) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << DIGIT))) != 0) );
			setState(379);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__14) {
				{
				setState(373);
				match(T__14);
				setState(375); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(374);
					hexdig();
					}
					}
					setState(377); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__24) | (1L << T__25) | (1L << T__27) | (1L << T__28) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << DIGIT))) != 0) );
				}
			}

			setState(381);
			_la = _input.LA(1);
			if ( !(_la==T__29 || _la==T__30) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(382);
			exponent();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FractionContext extends ParserRuleContext {
		public List<TerminalNode> DIGIT() { return getTokens(cddlParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(cddlParser.DIGIT, i);
		}
		public FractionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fraction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterFraction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitFraction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitFraction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FractionContext fraction() throws RecognitionException {
		FractionContext _localctx = new FractionContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_fraction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(385); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(384);
				match(DIGIT);
				}
				}
				setState(387); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DIGIT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExponentContext extends ParserRuleContext {
		public List<TerminalNode> DIGIT() { return getTokens(cddlParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(cddlParser.DIGIT, i);
		}
		public ExponentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exponent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterExponent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitExponent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitExponent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExponentContext exponent() throws RecognitionException {
		ExponentContext _localctx = new ExponentContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_exponent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__19 || _la==T__26) {
				{
				setState(389);
				_la = _input.LA(1);
				if ( !(_la==T__19 || _la==T__26) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(393); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(392);
				match(DIGIT);
				}
				}
				setState(395); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DIGIT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TextContext extends ParserRuleContext {
		public List<ScharContext> schar() {
			return getRuleContexts(ScharContext.class);
		}
		public ScharContext schar(int i) {
			return getRuleContext(ScharContext.class,i);
		}
		public TextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_text; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitText(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitText(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TextContext text() throws RecognitionException {
		TextContext _localctx = new TextContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_text);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(397);
			match(T__31);
			setState(401);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SCHAR || _la==SESC) {
				{
				{
				setState(398);
				schar();
				}
				}
				setState(403);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(404);
			match(T__31);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ScharContext extends ParserRuleContext {
		public TerminalNode SCHAR() { return getToken(cddlParser.SCHAR, 0); }
		public TerminalNode SESC() { return getToken(cddlParser.SESC, 0); }
		public ScharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterSchar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitSchar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitSchar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScharContext schar() throws RecognitionException {
		ScharContext _localctx = new ScharContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_schar);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(406);
			_la = _input.LA(1);
			if ( !(_la==SCHAR || _la==SESC) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BytesContext extends ParserRuleContext {
		public TerminalNode BSQUAL() { return getToken(cddlParser.BSQUAL, 0); }
		public List<BcharContext> bchar() {
			return getRuleContexts(BcharContext.class);
		}
		public BcharContext bchar(int i) {
			return getRuleContext(BcharContext.class,i);
		}
		public BytesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bytes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterBytes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitBytes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitBytes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BytesContext bytes() throws RecognitionException {
		BytesContext _localctx = new BytesContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_bytes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==BSQUAL) {
				{
				setState(408);
				match(BSQUAL);
				}
			}

			setState(411);
			match(T__32);
			setState(415);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SESC) | (1L << BCHAR) | (1L << CRLF))) != 0)) {
				{
				{
				setState(412);
				bchar();
				}
				}
				setState(417);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(418);
			match(T__32);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BcharContext extends ParserRuleContext {
		public TerminalNode BCHAR() { return getToken(cddlParser.BCHAR, 0); }
		public TerminalNode SESC() { return getToken(cddlParser.SESC, 0); }
		public TerminalNode CRLF() { return getToken(cddlParser.CRLF, 0); }
		public BcharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bchar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterBchar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitBchar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitBchar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BcharContext bchar() throws RecognitionException {
		BcharContext _localctx = new BcharContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_bchar);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(420);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SESC) | (1L << BCHAR) | (1L << CRLF))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public List<EalphaContext> ealpha() {
			return getRuleContexts(EalphaContext.class);
		}
		public EalphaContext ealpha(int i) {
			return getRuleContext(EalphaContext.class,i);
		}
		public List<TerminalNode> DIGIT() { return getTokens(cddlParser.DIGIT); }
		public TerminalNode DIGIT(int i) {
			return getToken(cddlParser.DIGIT, i);
		}
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_id);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(422);
			ealpha();
			setState(435);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(426);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__14 || _la==T__26) {
						{
						{
						setState(423);
						_la = _input.LA(1);
						if ( !(_la==T__14 || _la==T__26) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
						}
						setState(428);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(431);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case T__33:
					case T__34:
					case T__35:
					case ALPHA:
						{
						setState(429);
						ealpha();
						}
						break;
					case DIGIT:
						{
						setState(430);
						match(DIGIT);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					} 
				}
				setState(437);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EalphaContext extends ParserRuleContext {
		public TerminalNode ALPHA() { return getToken(cddlParser.ALPHA, 0); }
		public EalphaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ealpha; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterEalpha(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitEalpha(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitEalpha(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EalphaContext ealpha() throws RecognitionException {
		EalphaContext _localctx = new EalphaContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_ealpha);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(438);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << ALPHA))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HexdigContext extends ParserRuleContext {
		public TerminalNode DIGIT() { return getToken(cddlParser.DIGIT, 0); }
		public HexdigContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hexdig; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterHexdig(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitHexdig(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitHexdig(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HexdigContext hexdig() throws RecognitionException {
		HexdigContext _localctx = new HexdigContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_hexdig);
		int _la;
		try {
			setState(447);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DIGIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(440);
				match(DIGIT);
				}
				break;
			case T__36:
			case T__37:
				enterOuterAlt(_localctx, 2);
				{
				setState(441);
				_la = _input.LA(1);
				if ( !(_la==T__36 || _la==T__37) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case T__24:
			case T__25:
				enterOuterAlt(_localctx, 3);
				{
				setState(442);
				_la = _input.LA(1);
				if ( !(_la==T__24 || _la==T__25) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case T__38:
			case T__39:
				enterOuterAlt(_localctx, 4);
				{
				setState(443);
				_la = _input.LA(1);
				if ( !(_la==T__38 || _la==T__39) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case T__40:
			case T__41:
				enterOuterAlt(_localctx, 5);
				{
				setState(444);
				_la = _input.LA(1);
				if ( !(_la==T__40 || _la==T__41) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case T__27:
			case T__28:
				enterOuterAlt(_localctx, 6);
				{
				setState(445);
				_la = _input.LA(1);
				if ( !(_la==T__27 || _la==T__28) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case T__42:
			case T__43:
				enterOuterAlt(_localctx, 7);
				{
				setState(446);
				_la = _input.LA(1);
				if ( !(_la==T__42 || _la==T__43) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SContext extends ParserRuleContext {
		public List<WsContext> ws() {
			return getRuleContexts(WsContext.class);
		}
		public WsContext ws(int i) {
			return getRuleContext(WsContext.class,i);
		}
		public SContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_s; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterS(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitS(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitS(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SContext s() throws RecognitionException {
		SContext _localctx = new SContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_s);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(452);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(449);
					ws();
					}
					} 
				}
				setState(454);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WsContext extends ParserRuleContext {
		public TerminalNode SP() { return getToken(cddlParser.SP, 0); }
		public NlContext nl() {
			return getRuleContext(NlContext.class,0);
		}
		public WsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ws; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterWs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitWs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitWs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WsContext ws() throws RecognitionException {
		WsContext _localctx = new WsContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_ws);
		try {
			setState(457);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SP:
				enterOuterAlt(_localctx, 1);
				{
				setState(455);
				match(SP);
				}
				break;
			case T__44:
			case CRLF:
				enterOuterAlt(_localctx, 2);
				{
				setState(456);
				nl();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NlContext extends ParserRuleContext {
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public TerminalNode CRLF() { return getToken(cddlParser.CRLF, 0); }
		public NlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterNl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitNl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitNl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NlContext nl() throws RecognitionException {
		NlContext _localctx = new NlContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_nl);
		try {
			setState(461);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__44:
				enterOuterAlt(_localctx, 1);
				{
				setState(459);
				comment();
				}
				break;
			case CRLF:
				enterOuterAlt(_localctx, 2);
				{
				setState(460);
				match(CRLF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CommentContext extends ParserRuleContext {
		public TerminalNode CRLF() { return getToken(cddlParser.CRLF, 0); }
		public List<TerminalNode> PCHAR() { return getTokens(cddlParser.PCHAR); }
		public TerminalNode PCHAR(int i) {
			return getToken(cddlParser.PCHAR, i);
		}
		public CommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).enterComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof cddlListener ) ((cddlListener)listener).exitComment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof cddlVisitor ) return ((cddlVisitor<? extends T>)visitor).visitComment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentContext comment() throws RecognitionException {
		CommentContext _localctx = new CommentContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_comment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(463);
			match(T__44);
			setState(467);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PCHAR) {
				{
				{
				setState(464);
				match(PCHAR);
				}
				}
				setState(469);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(470);
			match(CRLF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3=\u01db\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\3\2\6\2M\n\2\r\2\16\2N\3\3\3\3\5\3"+
		"S\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\\\n\3\3\3\3\3\3\3\3\3\3\3\5\3c\n"+
		"\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6r\n\6\f\6\16"+
		"\6u\13\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\7\7\u0082\n\7\f\7"+
		"\16\7\u0085\13\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\7\b\u008f\n\b\f\b\16"+
		"\b\u0092\13\b\3\t\3\t\3\t\3\t\5\t\u0098\n\t\3\t\3\t\3\t\5\t\u009d\n\t"+
		"\3\n\3\n\3\n\5\n\u00a2\n\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00ba\n\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00c8\n\n\3\n\3\n\3\n\3\n\5\n"+
		"\u00ce\n\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00da\n\n\3\n\5"+
		"\n\u00dd\n\n\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\7\f\u00ea"+
		"\n\f\f\f\16\f\u00ed\13\f\3\r\3\r\3\r\7\r\u00f2\n\r\f\r\16\r\u00f5\13\r"+
		"\3\16\3\16\3\16\5\16\u00fa\n\16\3\16\3\16\3\16\5\16\u00ff\n\16\3\16\3"+
		"\16\3\16\3\16\5\16\u0105\n\16\3\16\3\16\5\16\u0109\n\16\3\16\3\16\3\16"+
		"\5\16\u010e\n\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0116\n\16\3\17\3"+
		"\17\3\17\3\17\5\17\u011c\n\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\5\17\u0129\n\17\3\20\3\20\3\21\3\21\3\21\5\21\u0130\n"+
		"\21\3\22\5\22\u0133\n\22\3\22\3\22\5\22\u0137\n\22\3\22\3\22\5\22\u013b"+
		"\n\22\3\23\3\23\7\23\u013f\n\23\f\23\16\23\u0142\13\23\3\23\3\23\3\23"+
		"\3\23\6\23\u0148\n\23\r\23\16\23\u0149\3\23\3\23\3\23\3\23\6\23\u0150"+
		"\n\23\r\23\16\23\u0151\3\23\5\23\u0155\n\23\3\24\3\24\3\24\5\24\u015a"+
		"\n\24\3\25\5\25\u015d\n\25\3\25\3\25\3\26\3\26\3\26\3\26\5\26\u0165\n"+
		"\26\3\26\3\26\5\26\u0169\n\26\5\26\u016b\n\26\3\27\5\27\u016e\n\27\3\27"+
		"\3\27\3\27\3\27\6\27\u0174\n\27\r\27\16\27\u0175\3\27\3\27\6\27\u017a"+
		"\n\27\r\27\16\27\u017b\5\27\u017e\n\27\3\27\3\27\3\27\3\30\6\30\u0184"+
		"\n\30\r\30\16\30\u0185\3\31\5\31\u0189\n\31\3\31\6\31\u018c\n\31\r\31"+
		"\16\31\u018d\3\32\3\32\7\32\u0192\n\32\f\32\16\32\u0195\13\32\3\32\3\32"+
		"\3\33\3\33\3\34\5\34\u019c\n\34\3\34\3\34\7\34\u01a0\n\34\f\34\16\34\u01a3"+
		"\13\34\3\34\3\34\3\35\3\35\3\36\3\36\7\36\u01ab\n\36\f\36\16\36\u01ae"+
		"\13\36\3\36\3\36\5\36\u01b2\n\36\7\36\u01b4\n\36\f\36\16\36\u01b7\13\36"+
		"\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \5 \u01c2\n \3!\7!\u01c5\n!\f!\16!\u01c8"+
		"\13!\3\"\3\"\5\"\u01cc\n\"\3#\3#\5#\u01d0\n#\3$\3$\7$\u01d4\n$\f$\16$"+
		"\u01d7\13$\3$\3$\3$\2\2%\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&("+
		"*,.\60\62\64\668:<>@BDF\2\17\3\2\31\32\3\2\33\34\3\2\36\37\3\2 !\4\2\26"+
		"\26\35\35\3\2\63\64\4\2\64\65==\4\2\21\21\35\35\4\2$&\67\67\3\2\'(\3\2"+
		")*\3\2+,\3\2-.\2\u0203\2H\3\2\2\2\4b\3\2\2\2\6d\3\2\2\2\bf\3\2\2\2\nh"+
		"\3\2\2\2\fx\3\2\2\2\16\u0088\3\2\2\2\20\u0093\3\2\2\2\22\u00dc\3\2\2\2"+
		"\24\u00de\3\2\2\2\26\u00e1\3\2\2\2\30\u00f3\3\2\2\2\32\u0115\3\2\2\2\34"+
		"\u0128\3\2\2\2\36\u012a\3\2\2\2 \u012c\3\2\2\2\"\u013a\3\2\2\2$\u0154"+
		"\3\2\2\2&\u0159\3\2\2\2(\u015c\3\2\2\2*\u016a\3\2\2\2,\u016d\3\2\2\2."+
		"\u0183\3\2\2\2\60\u0188\3\2\2\2\62\u018f\3\2\2\2\64\u0198\3\2\2\2\66\u019b"+
		"\3\2\2\28\u01a6\3\2\2\2:\u01a8\3\2\2\2<\u01b8\3\2\2\2>\u01c1\3\2\2\2@"+
		"\u01c6\3\2\2\2B\u01cb\3\2\2\2D\u01cf\3\2\2\2F\u01d1\3\2\2\2HL\5@!\2IJ"+
		"\5\4\3\2JK\5@!\2KM\3\2\2\2LI\3\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2\2O\3"+
		"\3\2\2\2PR\5\6\4\2QS\5\n\6\2RQ\3\2\2\2RS\3\2\2\2ST\3\2\2\2TU\5@!\2UV\7"+
		"\60\2\2VW\5@!\2WX\5\16\b\2Xc\3\2\2\2Y[\5\b\5\2Z\\\5\n\6\2[Z\3\2\2\2[\\"+
		"\3\2\2\2\\]\3\2\2\2]^\5@!\2^_\7\61\2\2_`\5@!\2`a\5\32\16\2ac\3\2\2\2b"+
		"P\3\2\2\2bY\3\2\2\2c\5\3\2\2\2de\5:\36\2e\7\3\2\2\2fg\5:\36\2g\t\3\2\2"+
		"\2hi\7\3\2\2ij\5@!\2jk\5:\36\2ks\5@!\2lm\7\4\2\2mn\5@!\2no\5:\36\2op\5"+
		"@!\2pr\3\2\2\2ql\3\2\2\2ru\3\2\2\2sq\3\2\2\2st\3\2\2\2tv\3\2\2\2us\3\2"+
		"\2\2vw\7\5\2\2w\13\3\2\2\2xy\7\3\2\2yz\5@!\2z{\5\20\t\2{\u0083\5@!\2|"+
		"}\7\4\2\2}~\5@!\2~\177\5\20\t\2\177\u0080\5@!\2\u0080\u0082\3\2\2\2\u0081"+
		"|\3\2\2\2\u0082\u0085\3\2\2\2\u0083\u0081\3\2\2\2\u0083\u0084\3\2\2\2"+
		"\u0084\u0086\3\2\2\2\u0085\u0083\3\2\2\2\u0086\u0087\7\5\2\2\u0087\r\3"+
		"\2\2\2\u0088\u0090\5\20\t\2\u0089\u008a\5@!\2\u008a\u008b\7\6\2\2\u008b"+
		"\u008c\5@!\2\u008c\u008d\5\20\t\2\u008d\u008f\3\2\2\2\u008e\u0089\3\2"+
		"\2\2\u008f\u0092\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091"+
		"\17\3\2\2\2\u0092\u0090\3\2\2\2\u0093\u009c\5\22\n\2\u0094\u0097\5@!\2"+
		"\u0095\u0098\7\62\2\2\u0096\u0098\5\24\13\2\u0097\u0095\3\2\2\2\u0097"+
		"\u0096\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u009a\5@!\2\u009a\u009b\5\22"+
		"\n\2\u009b\u009d\3\2\2\2\u009c\u0094\3\2\2\2\u009c\u009d\3\2\2\2\u009d"+
		"\21\3\2\2\2\u009e\u00dd\5&\24\2\u009f\u00a1\5\6\4\2\u00a0\u00a2\5\f\7"+
		"\2\u00a1\u00a0\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00dd\3\2\2\2\u00a3\u00a4"+
		"\7\7\2\2\u00a4\u00a5\5@!\2\u00a5\u00a6\5\16\b\2\u00a6\u00a7\5@!\2\u00a7"+
		"\u00a8\7\b\2\2\u00a8\u00dd\3\2\2\2\u00a9\u00aa\7\t\2\2\u00aa\u00ab\5@"+
		"!\2\u00ab\u00ac\5\26\f\2\u00ac\u00ad\5@!\2\u00ad\u00ae\7\n\2\2\u00ae\u00dd"+
		"\3\2\2\2\u00af\u00b0\7\13\2\2\u00b0\u00b1\5@!\2\u00b1\u00b2\5\26\f\2\u00b2"+
		"\u00b3\5@!\2\u00b3\u00b4\7\f\2\2\u00b4\u00dd\3\2\2\2\u00b5\u00b6\7\r\2"+
		"\2\u00b6\u00b7\5@!\2\u00b7\u00b9\5\6\4\2\u00b8\u00ba\5\f\7\2\u00b9\u00b8"+
		"\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00dd\3\2\2\2\u00bb\u00bc\7\16\2\2"+
		"\u00bc\u00bd\5@!\2\u00bd\u00be\7\7\2\2\u00be\u00bf\5@!\2\u00bf\u00c0\5"+
		"\26\f\2\u00c0\u00c1\5@!\2\u00c1\u00c2\7\b\2\2\u00c2\u00dd\3\2\2\2\u00c3"+
		"\u00c4\7\16\2\2\u00c4\u00c5\5@!\2\u00c5\u00c7\5\b\5\2\u00c6\u00c8\5\f"+
		"\7\2\u00c7\u00c6\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00dd\3\2\2\2\u00c9"+
		"\u00ca\7\17\2\2\u00ca\u00cd\7\20\2\2\u00cb\u00cc\7\21\2\2\u00cc\u00ce"+
		"\5$\23\2\u00cd\u00cb\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf"+
		"\u00d0\7\7\2\2\u00d0\u00d1\5@!\2\u00d1\u00d2\5\16\b\2\u00d2\u00d3\5@!"+
		"\2\u00d3\u00d4\7\b\2\2\u00d4\u00dd\3\2\2\2\u00d5\u00d6\7\17\2\2\u00d6"+
		"\u00d9\78\2\2\u00d7\u00d8\7\21\2\2\u00d8\u00da\5$\23\2\u00d9\u00d7\3\2"+
		"\2\2\u00d9\u00da\3\2\2\2\u00da\u00dd\3\2\2\2\u00db\u00dd\7\17\2\2\u00dc"+
		"\u009e\3\2\2\2\u00dc\u009f\3\2\2\2\u00dc\u00a3\3\2\2\2\u00dc\u00a9\3\2"+
		"\2\2\u00dc\u00af\3\2\2\2\u00dc\u00b5\3\2\2\2\u00dc\u00bb\3\2\2\2\u00dc"+
		"\u00c3\3\2\2\2\u00dc\u00c9\3\2\2\2\u00dc\u00d5\3\2\2\2\u00dc\u00db\3\2"+
		"\2\2\u00dd\23\3\2\2\2\u00de\u00df\7\21\2\2\u00df\u00e0\5:\36\2\u00e0\25"+
		"\3\2\2\2\u00e1\u00eb\5\30\r\2\u00e2\u00e3\5@!\2\u00e3\u00e4\7\6\2\2\u00e4"+
		"\u00e5\7\6\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\5@!\2\u00e7\u00e8\5\30"+
		"\r\2\u00e8\u00ea\3\2\2\2\u00e9\u00e2\3\2\2\2\u00ea\u00ed\3\2\2\2\u00eb"+
		"\u00e9\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\27\3\2\2\2\u00ed\u00eb\3\2\2"+
		"\2\u00ee\u00ef\5\32\16\2\u00ef\u00f0\5 \21\2\u00f0\u00f2\3\2\2\2\u00f1"+
		"\u00ee\3\2\2\2\u00f2\u00f5\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2"+
		"\2\2\u00f4\31\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f6\u00f7\5\"\22\2\u00f7\u00f8"+
		"\5@!\2\u00f8\u00fa\3\2\2\2\u00f9\u00f6\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa"+
		"\u00fe\3\2\2\2\u00fb\u00fc\5\34\17\2\u00fc\u00fd\5@!\2\u00fd\u00ff\3\2"+
		"\2\2\u00fe\u00fb\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0100\3\2\2\2\u0100"+
		"\u0116\5\16\b\2\u0101\u0102\5\"\22\2\u0102\u0103\5@!\2\u0103\u0105\3\2"+
		"\2\2\u0104\u0101\3\2\2\2\u0104\u0105\3\2\2\2\u0105\u0106\3\2\2\2\u0106"+
		"\u0108\5\b\5\2\u0107\u0109\5\f\7\2\u0108\u0107\3\2\2\2\u0108\u0109\3\2"+
		"\2\2\u0109\u0116\3\2\2\2\u010a\u010b\5\"\22\2\u010b\u010c\5@!\2\u010c"+
		"\u010e\3\2\2\2\u010d\u010a\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u010f\3\2"+
		"\2\2\u010f\u0110\7\7\2\2\u0110\u0111\5@!\2\u0111\u0112\5\26\f\2\u0112"+
		"\u0113\5@!\2\u0113\u0114\7\b\2\2\u0114\u0116\3\2\2\2\u0115\u00f9\3\2\2"+
		"\2\u0115\u0104\3\2\2\2\u0115\u010d\3\2\2\2\u0116\33\3\2\2\2\u0117\u0118"+
		"\5\20\t\2\u0118\u011b\5@!\2\u0119\u011a\7\22\2\2\u011a\u011c\5@!\2\u011b"+
		"\u0119\3\2\2\2\u011b\u011c\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011e\7\23"+
		"\2\2\u011e\u011f\7\5\2\2\u011f\u0129\3\2\2\2\u0120\u0121\5\36\20\2\u0121"+
		"\u0122\5@!\2\u0122\u0123\7\24\2\2\u0123\u0129\3\2\2\2\u0124\u0125\5&\24"+
		"\2\u0125\u0126\5@!\2\u0126\u0127\7\24\2\2\u0127\u0129\3\2\2\2\u0128\u0117"+
		"\3\2\2\2\u0128\u0120\3\2\2\2\u0128\u0124\3\2\2\2\u0129\35\3\2\2\2\u012a"+
		"\u012b\5:\36\2\u012b\37\3\2\2\2\u012c\u012f\5@!\2\u012d\u012e\7\4\2\2"+
		"\u012e\u0130\5@!\2\u012f\u012d\3\2\2\2\u012f\u0130\3\2\2\2\u0130!\3\2"+
		"\2\2\u0131\u0133\5$\23\2\u0132\u0131\3\2\2\2\u0132\u0133\3\2\2\2\u0133"+
		"\u0134\3\2\2\2\u0134\u0136\7\25\2\2\u0135\u0137\5$\23\2\u0136\u0135\3"+
		"\2\2\2\u0136\u0137\3\2\2\2\u0137\u013b\3\2\2\2\u0138\u013b\7\26\2\2\u0139"+
		"\u013b\7\27\2\2\u013a\u0132\3\2\2\2\u013a\u0138\3\2\2\2\u013a\u0139\3"+
		"\2\2\2\u013b#\3\2\2\2\u013c\u0140\79\2\2\u013d\u013f\78\2\2\u013e\u013d"+
		"\3\2\2\2\u013f\u0142\3\2\2\2\u0140\u013e\3\2\2\2\u0140\u0141\3\2\2\2\u0141"+
		"\u0155\3\2\2\2\u0142\u0140\3\2\2\2\u0143\u0144\7\30\2\2\u0144\u0145\t"+
		"\2\2\2\u0145\u0147\3\2\2\2\u0146\u0148\5> \2\u0147\u0146\3\2\2\2\u0148"+
		"\u0149\3\2\2\2\u0149\u0147\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u0155\3\2"+
		"\2\2\u014b\u014c\7\30\2\2\u014c\u014d\t\3\2\2\u014d\u014f\3\2\2\2\u014e"+
		"\u0150\7:\2\2\u014f\u014e\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u014f\3\2"+
		"\2\2\u0151\u0152\3\2\2\2\u0152\u0155\3\2\2\2\u0153\u0155\7\30\2\2\u0154"+
		"\u013c\3\2\2\2\u0154\u0143\3\2\2\2\u0154\u014b\3\2\2\2\u0154\u0153\3\2"+
		"\2\2\u0155%\3\2\2\2\u0156\u015a\5*\26\2\u0157\u015a\5\62\32\2\u0158\u015a"+
		"\5\66\34\2\u0159\u0156\3\2\2\2\u0159\u0157\3\2\2\2\u0159\u0158\3\2\2\2"+
		"\u015a\'\3\2\2\2\u015b\u015d\7\35\2\2\u015c\u015b\3\2\2\2\u015c\u015d"+
		"\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015f\5$\23\2\u015f)\3\2\2\2\u0160"+
		"\u016b\5,\27\2\u0161\u0164\5(\25\2\u0162\u0163\7\21\2\2\u0163\u0165\5"+
		".\30\2\u0164\u0162\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0168\3\2\2\2\u0166"+
		"\u0167\t\4\2\2\u0167\u0169\5\60\31\2\u0168\u0166\3\2\2\2\u0168\u0169\3"+
		"\2\2\2\u0169\u016b\3\2\2\2\u016a\u0160\3\2\2\2\u016a\u0161\3\2\2\2\u016b"+
		"+\3\2\2\2\u016c\u016e\7\35\2\2\u016d\u016c\3\2\2\2\u016d\u016e\3\2\2\2"+
		"\u016e\u016f\3\2\2\2\u016f\u0170\7\30\2\2\u0170\u0171\t\2\2\2\u0171\u0173"+
		"\3\2\2\2\u0172\u0174\5> \2\u0173\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175"+
		"\u0173\3\2\2\2\u0175\u0176\3\2\2\2\u0176\u017d\3\2\2\2\u0177\u0179\7\21"+
		"\2\2\u0178\u017a\5> \2\u0179\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u0179"+
		"\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017e\3\2\2\2\u017d\u0177\3\2\2\2\u017d"+
		"\u017e\3\2\2\2\u017e\u017f\3\2\2\2\u017f\u0180\t\5\2\2\u0180\u0181\5\60"+
		"\31\2\u0181-\3\2\2\2\u0182\u0184\78\2\2\u0183\u0182\3\2\2\2\u0184\u0185"+
		"\3\2\2\2\u0185\u0183\3\2\2\2\u0185\u0186\3\2\2\2\u0186/\3\2\2\2\u0187"+
		"\u0189\t\6\2\2\u0188\u0187\3\2\2\2\u0188\u0189\3\2\2\2\u0189\u018b\3\2"+
		"\2\2\u018a\u018c\78\2\2\u018b\u018a\3\2\2\2\u018c\u018d\3\2\2\2\u018d"+
		"\u018b\3\2\2\2\u018d\u018e\3\2\2\2\u018e\61\3\2\2\2\u018f\u0193\7\"\2"+
		"\2\u0190\u0192\5\64\33\2\u0191\u0190\3\2\2\2\u0192\u0195\3\2\2\2\u0193"+
		"\u0191\3\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2\2\2\u0195\u0193\3\2"+
		"\2\2\u0196\u0197\7\"\2\2\u0197\63\3\2\2\2\u0198\u0199\t\7\2\2\u0199\65"+
		"\3\2\2\2\u019a\u019c\7\66\2\2\u019b\u019a\3\2\2\2\u019b\u019c\3\2\2\2"+
		"\u019c\u019d\3\2\2\2\u019d\u01a1\7#\2\2\u019e\u01a0\58\35\2\u019f\u019e"+
		"\3\2\2\2\u01a0\u01a3\3\2\2\2\u01a1\u019f\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2"+
		"\u01a4\3\2\2\2\u01a3\u01a1\3\2\2\2\u01a4\u01a5\7#\2\2\u01a5\67\3\2\2\2"+
		"\u01a6\u01a7\t\b\2\2\u01a79\3\2\2\2\u01a8\u01b5\5<\37\2\u01a9\u01ab\t"+
		"\t\2\2\u01aa\u01a9\3\2\2\2\u01ab\u01ae\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac"+
		"\u01ad\3\2\2\2\u01ad\u01b1\3\2\2\2\u01ae\u01ac\3\2\2\2\u01af\u01b2\5<"+
		"\37\2\u01b0\u01b2\78\2\2\u01b1\u01af\3\2\2\2\u01b1\u01b0\3\2\2\2\u01b2"+
		"\u01b4\3\2\2\2\u01b3\u01ac\3\2\2\2\u01b4\u01b7\3\2\2\2\u01b5\u01b3\3\2"+
		"\2\2\u01b5\u01b6\3\2\2\2\u01b6;\3\2\2\2\u01b7\u01b5\3\2\2\2\u01b8\u01b9"+
		"\t\n\2\2\u01b9=\3\2\2\2\u01ba\u01c2\78\2\2\u01bb\u01c2\t\13\2\2\u01bc"+
		"\u01c2\t\3\2\2\u01bd\u01c2\t\f\2\2\u01be\u01c2\t\r\2\2\u01bf\u01c2\t\4"+
		"\2\2\u01c0\u01c2\t\16\2\2\u01c1\u01ba\3\2\2\2\u01c1\u01bb\3\2\2\2\u01c1"+
		"\u01bc\3\2\2\2\u01c1\u01bd\3\2\2\2\u01c1\u01be\3\2\2\2\u01c1\u01bf\3\2"+
		"\2\2\u01c1\u01c0\3\2\2\2\u01c2?\3\2\2\2\u01c3\u01c5\5B\"\2\u01c4\u01c3"+
		"\3\2\2\2\u01c5\u01c8\3\2\2\2\u01c6\u01c4\3\2\2\2\u01c6\u01c7\3\2\2\2\u01c7"+
		"A\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c9\u01cc\7;\2\2\u01ca\u01cc\5D#\2\u01cb"+
		"\u01c9\3\2\2\2\u01cb\u01ca\3\2\2\2\u01ccC\3\2\2\2\u01cd\u01d0\5F$\2\u01ce"+
		"\u01d0\7=\2\2\u01cf\u01cd\3\2\2\2\u01cf\u01ce\3\2\2\2\u01d0E\3\2\2\2\u01d1"+
		"\u01d5\7/\2\2\u01d2\u01d4\7<\2\2\u01d3\u01d2\3\2\2\2\u01d4\u01d7\3\2\2"+
		"\2\u01d5\u01d3\3\2\2\2\u01d5\u01d6\3\2\2\2\u01d6\u01d8\3\2\2\2\u01d7\u01d5"+
		"\3\2\2\2\u01d8\u01d9\7=\2\2\u01d9G\3\2\2\2:NR[bs\u0083\u0090\u0097\u009c"+
		"\u00a1\u00b9\u00c7\u00cd\u00d9\u00dc\u00eb\u00f3\u00f9\u00fe\u0104\u0108"+
		"\u010d\u0115\u011b\u0128\u012f\u0132\u0136\u013a\u0140\u0149\u0151\u0154"+
		"\u0159\u015c\u0164\u0168\u016a\u016d\u0175\u017b\u017d\u0185\u0188\u018d"+
		"\u0193\u019b\u01a1\u01ac\u01b1\u01b5\u01c1\u01c6\u01cb\u01cf\u01d5";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}