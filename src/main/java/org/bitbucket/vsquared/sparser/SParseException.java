package org.bitbucket.vsquared.sparser;

import java.util.Arrays;

/**
 * Uma implementação de exception especifica para o {@link SParser}, com alguns detalhes do erro ocorrido durante o parsing
 * 
 * @author Vinicius de Lima Nogueira
 */
public class SParseException extends RuntimeException
{

	private static final long serialVersionUID = 5363804514310861893L;

	private final Integer position;
	private final String input;

	public SParseException()
	{
		this(null, null, null, null);
	}

	public SParseException(final String mensagem)
	{
		this(mensagem, null, null);
	}

	public SParseException(final String mensagem, final Integer thePosition, final String theInput)
	{
		this(mensagem, thePosition, theInput, null);
	}

	public SParseException(final Integer thePosition, final String theInput)
	{
		this(null, thePosition, theInput, null);
	}

	public SParseException(final Throwable causa)
	{
		this(null, null, null, causa);
	}

	public SParseException(final String mensagem, final Integer thePosition, final String theInput, final Throwable causa)
	{
		super(mensagem, causa);
		this.position = thePosition;
		this.input = theInput;
	}

	@Override
	public String getMessage()
	{
		if (position != null && input != null)
		{
			final char[] blankFiller = new char[position];
			Arrays.fill(blankFiller, ' ');

			String mensagem = super.getMessage();
			if (mensagem == null || mensagem.trim().isEmpty())
			{
				mensagem = "Token inesperado '" + input.charAt(position) + "' na posição " + (position + 1);
			}

			mensagem += "\n\n" + input.replaceAll("\n", "\\\\n") + "\n" + new String(blankFiller) + "^" + "\n";
			return mensagem;
		}
		return super.getMessage();
	}

	protected String getMessageOnly()
	{
		return super.getMessage();
	}
}
