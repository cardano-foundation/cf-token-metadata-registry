// Generated from /Users/sebastianbode/development/cardano/cf-metadata-server/java/core/src/main/antlr/cddl.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class cddlLexer extends Lexer {
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
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
			"T__17", "T__18", "T__19", "T__20", "T__21", "T__22", "T__23", "T__24", 
			"T__25", "T__26", "T__27", "T__28", "T__29", "T__30", "T__31", "T__32", 
			"T__33", "T__34", "T__35", "T__36", "T__37", "T__38", "T__39", "T__40", 
			"T__41", "T__42", "T__43", "T__44", "ASSIGNT", "ASSIGNG", "RANGEOP", 
			"SCHAR", "SESC", "BCHAR", "BSQUAL", "ALPHA", "DIGIT", "DIGIT1", "BINDIG", 
			"SP", "PCHAR", "CRLF"
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


	public cddlLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "cddl.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2=\u0106\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\3\2\3"+
		"\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13"+
		"\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22"+
		"\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31"+
		"\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!"+
		"\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3+\3+\3"+
		",\3,\3-\3-\3.\3.\3/\3/\3/\5/\u00d7\n/\3\60\3\60\3\60\3\60\5\60\u00dd\n"+
		"\60\3\61\3\61\3\61\3\61\3\61\5\61\u00e4\n\61\3\62\5\62\u00e7\n\62\3\63"+
		"\3\63\3\63\3\64\5\64\u00ed\n\64\3\65\3\65\3\65\3\65\5\65\u00f3\n\65\3"+
		"\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\5;\u0100\n;\3<\3<\3<\5<\u0105"+
		"\n<\2\2=\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17"+
		"\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\35"+
		"9\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66"+
		"k\67m8o9q:s;u<w=\3\2\5\4\2JJjj\4\2DDdd\4\2C\\c|\5\6\2\"\2#\2%\2]\2_\2"+
		"\u0080\2\u0082\2\uffff\22\4\2\"\2\u0080\2\u0082\2\uffff\22\5\2\"\2(\2"+
		"*\2]\2_\2\uffff\22\u010a\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2"+
		"\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25"+
		"\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2"+
		"\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2"+
		"\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3"+
		"\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2"+
		"\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2"+
		"Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3"+
		"\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2"+
		"\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2"+
		"w\3\2\2\2\3y\3\2\2\2\5{\3\2\2\2\7}\3\2\2\2\t\177\3\2\2\2\13\u0081\3\2"+
		"\2\2\r\u0083\3\2\2\2\17\u0085\3\2\2\2\21\u0087\3\2\2\2\23\u0089\3\2\2"+
		"\2\25\u008b\3\2\2\2\27\u008d\3\2\2\2\31\u008f\3\2\2\2\33\u0091\3\2\2\2"+
		"\35\u0093\3\2\2\2\37\u0095\3\2\2\2!\u0097\3\2\2\2#\u0099\3\2\2\2%\u009b"+
		"\3\2\2\2\'\u009d\3\2\2\2)\u009f\3\2\2\2+\u00a1\3\2\2\2-\u00a3\3\2\2\2"+
		"/\u00a5\3\2\2\2\61\u00a7\3\2\2\2\63\u00a9\3\2\2\2\65\u00ab\3\2\2\2\67"+
		"\u00ad\3\2\2\29\u00af\3\2\2\2;\u00b1\3\2\2\2=\u00b3\3\2\2\2?\u00b5\3\2"+
		"\2\2A\u00b7\3\2\2\2C\u00b9\3\2\2\2E\u00bb\3\2\2\2G\u00bd\3\2\2\2I\u00bf"+
		"\3\2\2\2K\u00c1\3\2\2\2M\u00c3\3\2\2\2O\u00c5\3\2\2\2Q\u00c7\3\2\2\2S"+
		"\u00c9\3\2\2\2U\u00cb\3\2\2\2W\u00cd\3\2\2\2Y\u00cf\3\2\2\2[\u00d1\3\2"+
		"\2\2]\u00d6\3\2\2\2_\u00dc\3\2\2\2a\u00e3\3\2\2\2c\u00e6\3\2\2\2e\u00e8"+
		"\3\2\2\2g\u00ec\3\2\2\2i\u00f2\3\2\2\2k\u00f4\3\2\2\2m\u00f6\3\2\2\2o"+
		"\u00f8\3\2\2\2q\u00fa\3\2\2\2s\u00fc\3\2\2\2u\u00ff\3\2\2\2w\u0104\3\2"+
		"\2\2yz\7>\2\2z\4\3\2\2\2{|\7.\2\2|\6\3\2\2\2}~\7@\2\2~\b\3\2\2\2\177\u0080"+
		"\7\61\2\2\u0080\n\3\2\2\2\u0081\u0082\7*\2\2\u0082\f\3\2\2\2\u0083\u0084"+
		"\7+\2\2\u0084\16\3\2\2\2\u0085\u0086\7}\2\2\u0086\20\3\2\2\2\u0087\u0088"+
		"\7\177\2\2\u0088\22\3\2\2\2\u0089\u008a\7]\2\2\u008a\24\3\2\2\2\u008b"+
		"\u008c\7_\2\2\u008c\26\3\2\2\2\u008d\u008e\7\u0080\2\2\u008e\30\3\2\2"+
		"\2\u008f\u0090\7(\2\2\u0090\32\3\2\2\2\u0091\u0092\7%\2\2\u0092\34\3\2"+
		"\2\2\u0093\u0094\78\2\2\u0094\36\3\2\2\2\u0095\u0096\7\60\2\2\u0096 \3"+
		"\2\2\2\u0097\u0098\7`\2\2\u0098\"\3\2\2\2\u0099\u009a\7?\2\2\u009a$\3"+
		"\2\2\2\u009b\u009c\7<\2\2\u009c&\3\2\2\2\u009d\u009e\7,\2\2\u009e(\3\2"+
		"\2\2\u009f\u00a0\7-\2\2\u00a0*\3\2\2\2\u00a1\u00a2\7A\2\2\u00a2,\3\2\2"+
		"\2\u00a3\u00a4\7\62\2\2\u00a4.\3\2\2\2\u00a5\u00a6\7Z\2\2\u00a6\60\3\2"+
		"\2\2\u00a7\u00a8\7z\2\2\u00a8\62\3\2\2\2\u00a9\u00aa\7D\2\2\u00aa\64\3"+
		"\2\2\2\u00ab\u00ac\7d\2\2\u00ac\66\3\2\2\2\u00ad\u00ae\7/\2\2\u00ae8\3"+
		"\2\2\2\u00af\u00b0\7G\2\2\u00b0:\3\2\2\2\u00b1\u00b2\7g\2\2\u00b2<\3\2"+
		"\2\2\u00b3\u00b4\7R\2\2\u00b4>\3\2\2\2\u00b5\u00b6\7r\2\2\u00b6@\3\2\2"+
		"\2\u00b7\u00b8\7$\2\2\u00b8B\3\2\2\2\u00b9\u00ba\7)\2\2\u00baD\3\2\2\2"+
		"\u00bb\u00bc\7B\2\2\u00bcF\3\2\2\2\u00bd\u00be\7a\2\2\u00beH\3\2\2\2\u00bf"+
		"\u00c0\7&\2\2\u00c0J\3\2\2\2\u00c1\u00c2\7C\2\2\u00c2L\3\2\2\2\u00c3\u00c4"+
		"\7c\2\2\u00c4N\3\2\2\2\u00c5\u00c6\7E\2\2\u00c6P\3\2\2\2\u00c7\u00c8\7"+
		"e\2\2\u00c8R\3\2\2\2\u00c9\u00ca\7F\2\2\u00caT\3\2\2\2\u00cb\u00cc\7f"+
		"\2\2\u00ccV\3\2\2\2\u00cd\u00ce\7H\2\2\u00ceX\3\2\2\2\u00cf\u00d0\7h\2"+
		"\2\u00d0Z\3\2\2\2\u00d1\u00d2\7=\2\2\u00d2\\\3\2\2\2\u00d3\u00d7\7?\2"+
		"\2\u00d4\u00d5\7\61\2\2\u00d5\u00d7\7?\2\2\u00d6\u00d3\3\2\2\2\u00d6\u00d4"+
		"\3\2\2\2\u00d7^\3\2\2\2\u00d8\u00dd\7?\2\2\u00d9\u00da\7\61\2\2\u00da"+
		"\u00db\7\61\2\2\u00db\u00dd\7?\2\2\u00dc\u00d8\3\2\2\2\u00dc\u00d9\3\2"+
		"\2\2\u00dd`\3\2\2\2\u00de\u00df\7\60\2\2\u00df\u00e0\7\60\2\2\u00e0\u00e4"+
		"\7\60\2\2\u00e1\u00e2\7\60\2\2\u00e2\u00e4\7\60\2\2\u00e3\u00de\3\2\2"+
		"\2\u00e3\u00e1\3\2\2\2\u00e4b\3\2\2\2\u00e5\u00e7\t\5\2\2\u00e6\u00e5"+
		"\3\2\2\2\u00e7d\3\2\2\2\u00e8\u00e9\7^\2\2\u00e9\u00ea\t\6\2\2\u00eaf"+
		"\3\2\2\2\u00eb\u00ed\t\7\2\2\u00ec\u00eb\3\2\2\2\u00edh\3\2\2\2\u00ee"+
		"\u00f3\t\2\2\2\u00ef\u00f0\t\3\2\2\u00f0\u00f1\78\2\2\u00f1\u00f3\7\66"+
		"\2\2\u00f2\u00ee\3\2\2\2\u00f2\u00ef\3\2\2\2\u00f3j\3\2\2\2\u00f4\u00f5"+
		"\t\4\2\2\u00f5l\3\2\2\2\u00f6\u00f7\4\62;\2\u00f7n\3\2\2\2\u00f8\u00f9"+
		"\4\63;\2\u00f9p\3\2\2\2\u00fa\u00fb\4\62\63\2\u00fbr\3\2\2\2\u00fc\u00fd"+
		"\7\"\2\2\u00fdt\3\2\2\2\u00fe\u0100\t\6\2\2\u00ff\u00fe\3\2\2\2\u0100"+
		"v\3\2\2\2\u0101\u0105\7\f\2\2\u0102\u0103\7\17\2\2\u0103\u0105\7\f\2\2"+
		"\u0104\u0101\3\2\2\2\u0104\u0102\3\2\2\2\u0105x\3\2\2\2\13\2\u00d6\u00dc"+
		"\u00e3\u00e6\u00ec\u00f2\u00ff\u0104\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}