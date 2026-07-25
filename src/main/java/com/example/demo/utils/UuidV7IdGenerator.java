package com.example.demo.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class UuidV7IdGenerator implements IdentifierGenerator {

		@Override
		public Object generate(SharedSessionContractImplementor session, Object obj) {
				return UuidV7Generator.generate();
		}
}
