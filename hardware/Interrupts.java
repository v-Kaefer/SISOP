package hardware;

public enum Interrupts {
	noInterrupt,
	INVALID_INSTRUCTION,
	INVALID_ADDRESS,
	OVERFLOW,
	SYSCALL,
	END;
	// Os nomes antigos como intEnderecoInvalido foram modernizados para melhor clareza.
	// intSTOP foi substituído por END.
}
