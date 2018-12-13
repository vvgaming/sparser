package org.bitbucket.vsquared.sparser.util;

/**
 * Simples utilitários
 * 
 * @author Vinicius de Lima Nogueira
 */
public final class Validations
{

	private Validations()
	{
		// só utilidades
	}

	/**
	 * Requer que a condição informada seja verdadeira, do contrário lançando exceção {@link RequiredConditionException}
	 * 
	 * @param condition a expressão lógica a ser avaliada
	 * @param erroMsg a mensagem que deve ser embutida na exceção
	 */
	public static void require(final boolean condition, final String erroMsg)
	{

		if (!condition)
		{
			throw new RequiredConditionException(erroMsg);
		}

	}

	public static class RequiredConditionException extends IllegalStateException
	{
		private static final long serialVersionUID = 5811333386690528070L;

		public RequiredConditionException(final String erroMsg)
		{
			super(erroMsg);
		}

	}

}
