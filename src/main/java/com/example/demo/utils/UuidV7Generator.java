package com.example.demo.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class UuidV7Generator {

		private UuidV7Generator() {
		}

		public static UUID generate() {
				long millis = System.currentTimeMillis();
				long random = ThreadLocalRandom.current().nextLong();

				long msb = (millis << 16) | 0x7000L | (random & 0x0FFFL);
				long lsb = (random >>> 2) | 0x8000000000000000L;

				return new UUID(msb, lsb);
		}
}
