package org.bitbucket.vsquared.sparser;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bitbucket.vsquared.sparser.util.Tuple2;

/**
 * Essa classe é a implementação de um parser, que dada uma String (<code>input</code>) realiza a extração de tokens baseado em suas regras
 * pré-definidas. <br/>
 * As regras são definidas em função de uma <code>Regex</code> que é passada na criação e/ou na combinação dessas regras utilizando os
 * métodos para este fim. <br/>
 * Por exemplo, o método {@link SParser#and(SParser) and} combina dois parsers de modo que o parser final obrigue a verificação das duas
 * regex.<br/>
 * <br/>
 * Este parser é particularmente útil para implementações de DSL's - Domain-Specific Language. São uma espécie de mini-linguagens que
 * resolvem problemas específicos. Veja <a href=http://en.wikipedia.org/wiki/Domain-specific_language>Domain-specific language</a> para mais
 * informações. <br/>
 * 
 * @author Vinicius de Lima Nogueira
 * @author Vinicius Seufitele Pinto
 */
public class SParser
{
	protected final SParser[] injectedParser;

	private final String regex;

	/**
	 * Parser vazio que sempre retorna uma lista vazia e não consome nada da input
	 */
	public static final SParser empty = new SParser()
	{
		@Override
		protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
		{
			return Tuple2.<List<Object>, Integer> from(new ArrayList<Object>(), startPos);
		}
	};

	private SParser()
	{
		this((String) null);
	}

	protected SParser(final String theRegex)
	{
		super();
		this.regex = theRegex;
		injectedParser = new SParser[1];
	}

	private SParser(final SParser[] theInjectedParser)
	{
		this.regex = null;
		injectedParser = theInjectedParser;
	}

	/**
	 * Cria um parser base com a <code>theRegex</code> como regra
	 * 
	 * @param theRegex
	 */
	public static SParser from(final String theRegex)
	{
		return new SParser(theRegex);
	}

	/**
	 * Combina o parser atual com o <code>parserAnd</code> usando uma conjunção lógica <i>(E/AND)</i>, isto é, o parser só vai retornar se
	 * as duas regras combinadas forem satisfeitas
	 * 
	 * @param parserAnd
	 */
	public SParser and(final SParser parserAnd)
	{

		return new SParser(injectedParser)
		{
			@Override
			protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
			{

				final Tuple2<List<Object>, Integer> firstResult = SParser.this.parse(input, startPos);
				final Tuple2<List<Object>, Integer> secondResult = parserAnd.parse(input, firstResult.getVal2());

				return Tuple2.from(Stream.concat(firstResult.getVal1().stream(), secondResult.getVal1().stream()).collect(toList()),
						secondResult.getVal2());
			}

		};
	}

	/**
	 * Combina o parser atual com o <code>parserOr</code> usando uma disjunção lógica <i>(OU/OR)</i>, isto é, o parser vai retornar se
	 * qualquer uma das duas regras forem satisfeitas
	 * 
	 * @param parserOr
	 */
	public SParser or(final SParser parserOr)
	{

		return new SParser(injectedParser)
		{
			@Override
			protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
			{
				try
				{
					return SParser.this.parse(input, startPos);
				}
				catch (final SParseException parEx)
				{
					return parserOr.parse(input, startPos);
				}
			}
		};
	}

	/**
	 * Combina o parser atual com ele mesmo, fazendo com que o novo parser retorne se as regras deste parser forem satisfeitas 0 ou N vezes,
	 * é um operador quantificador de repetição similar ao <code>*</code> da Regex
	 */
	public SParser repStar()
	{
		return rep(0);
	}

	/**
	 * Combina o parser atual com ele mesmo, fazendo com que o novo parser retorne se as regras deste parser forem satisfeitas 1 ou N vezes,
	 * é um operador quantificador de repetição similar ao <code>+</code> da Regex
	 */
	public SParser repPlus()
	{
		return rep(1);
	}

	/**
	 * Combina o parser atual com ele mesmo, fazendo com que o novo parser retorne se as regras deste parser forem satisfeitas
	 * <code>min</code> ou N vezes, é um operador quantificador de repetição similar a <code>{min,}</code> na Regex
	 * 
	 * @param min mínimo de ocorrências
	 */
	public SParser rep(final int min)
	{
		return rep(min, Integer.MAX_VALUE);
	}

	/**
	 * Combina o parser atual com ele mesmo, fazendo com que o novo parser retorne se as regras deste parser forem satisfeitas de
	 * <code>min</code> à <code>max</code> vezes, é um operador quantificador de repetição similar a <code>{min,max}</code> na Regex
	 * 
	 * @param min mínimo de ocorrências
	 * @param max máximo de ocorrências
	 */
	public SParser rep(final int min, final int max)
	{
		assert min >= 0;
		assert max >= min;

		return new SParser(injectedParser)
		{
			@Override
			protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
			{

				final List<Object> results = new ArrayList<Object>();
				Integer lastPos = startPos;
				int qtdOccur = 0;
				try
				{
					while (qtdOccur < max)
					{
						final Tuple2<List<Object>, Integer> result = SParser.this.parse(input, lastPos);
						lastPos = result.getVal2();
						results.addAll(result.getVal1());
						qtdOccur++;
					}
				}
				catch (final SParseException parEx)
				{
					// ignora, pois ja temos um resultado
				}

				if (qtdOccur < min)
				{
					throw new SParseException(lastPos, input);
				}

				return Tuple2.from(results, lastPos);
			}
		};
	}

	/**
	 * Marca o ponto, como sendo um ponto de injeção para um "parser futuro", depois de algumas operações você deve usar o método
	 * {@link SParser#inject() inject()} para injetar o parser neste ponto. <br/>
	 * É particularmente útil para se fazer parser recursivos
	 */
	public SParser markIPoint()
	{

		return new SParser(injectedParser)
		{
			@Override
			protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
			{

				final Tuple2<List<Object>, Integer> firstResult = SParser.this.parse(input, startPos);
				final Tuple2<List<Object>, Integer> secondResult = injectedParser[0].parse(input, firstResult.getVal2());

				return Tuple2.from(concat(firstResult.getVal1().stream(), secondResult.getVal1().stream()).collect(toList()),
						secondResult.getVal2());
			}

		};

	}

	/**
	 * Injeta o atual parser dentro de um Injection Point marcado anteriormente. Vide {@link SParser#markIPoint() markIPoint()}.
	 */
	public SParser inject()
	{
		injectedParser[0] = this;
		return this;
		// return new Parser()
		// {
		// @Override
		// public List<Object> parse(final String input)
		// {
		// return Parser.this.parse(input);
		// }
		// };
	}

	/**
	 * Mapea os resultados encontrados por este parse a outros utilizando a função de mapeamento passada por parametro
	 * 
	 * @param func função de mapeamento
	 */
	public SParser map(final Function<List<Object>, List<Object>> func)
	{
		return new SParser(injectedParser)
		{
			@Override
			protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
			{
				final Tuple2<List<Object>, Integer> parseResult = SParser.this.parse(input, startPos);
				return Tuple2.from(func.apply(parseResult.getVal1()), parseResult.getVal2());
			}

		};
	}

	/**
	 * Mapea os resultados encontrados por este parse para uma função de descarte, isto é, descarta os resultados encontrados por este
	 * parser
	 */
	public SParser mapDiscard()
	{
		return map(new Function<List<Object>, List<Object>>()
		{
			@Override
			public List<Object> apply(final List<Object> param)
			{
				return new ArrayList<Object>();
			}
		});
	}

	/**
	 * Efetua o parse propriamente dito da input
	 * 
	 * @param input entrada que será informada
	 * @return Lista de itens encontrados de acordo com a regra definida neste parser
	 */
	public List<Object> parse(final String input)
	{

		final Tuple2<List<Object>, Integer> parseResult = parse(input, 0);
		if (parseResult.getVal2() == input.length())
		{
			return parseResult.getVal1();
		}

		// Falhou por que não varreu toda a input
		throw new SParseException(parseResult.getVal2(), input);
	}

	protected Tuple2<List<Object>, Integer> parse(final String input, final Integer startPos)
	{

		if (startPos >= input.length())
		{
			throw new SParseException("Tentando dar parse além dos limites da input");
		}

		final Pattern pattern = Pattern.compile("^(" + regex + ").*");
		final Matcher matcher = pattern.matcher(input.substring(startPos));

		if (matcher.find())
		{
			final String value = matcher.group(1);
			return Tuple2.from(Arrays.<Object> asList(value), startPos + value.length());
		}

		// Falhou por que não encontrou nenhum token
		throw new SParseException(startPos, input);
	}

}
