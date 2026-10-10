package com.example.demo.config.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Window;

class CursorsTest {

	@Test
	void encodeThenDecode_roundTrips() {
		var id = UUID.randomUUID();
		assertThat(Cursors.decode(Cursors.encode(id))).isEqualTo(id);
	}

	@Test
	void decode_nullOrBlank_meansStart() {
		assertThat(Cursors.decode(null)).isNull();
		assertThat(Cursors.decode(" ")).isNull();
	}

	@Test
	void decode_garbage_isRejected() {
		assertThatThrownBy(() -> Cursors.decode("not a cursor")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Cursors.decode("aGVsbG8")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Cursors.decode(java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString("id:nope".getBytes()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void pageSize_defaultsAndLimits() {
		assertThat(Cursors.pageSize(null)).isEqualTo(Cursors.DEFAULT_PAGE_SIZE);
		assertThat(Cursors.pageSize(1)).isEqualTo(1);
		assertThat(Cursors.pageSize(Cursors.MAX_PAGE_SIZE)).isEqualTo(Cursors.MAX_PAGE_SIZE);
		assertThatThrownBy(() -> Cursors.pageSize(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Cursors.pageSize(-5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Cursors.pageSize(Cursors.MAX_PAGE_SIZE + 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void toConnection_buildsEdgesAndPageInfo() {
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		var connection = Cursors.toConnection(Window.from(List.of(first, second), i -> null, true),
				id -> id, Cursors.encode(UUID.randomUUID()));

		assertThat(connection.edges()).hasSize(2);
		assertThat(connection.pageInfo().hasNextPage()).isTrue();
		assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
		assertThat(connection.pageInfo().startCursor()).isEqualTo(Cursors.encode(first));
		assertThat(connection.pageInfo().endCursor()).isEqualTo(Cursors.encode(second));
	}

	@Test
	void toConnection_emptyWindow_hasNoCursors() {
		var connection = Cursors.toConnection(Window.<UUID>from(List.of(), i -> null, false), id -> id, null);
		assertThat(connection.edges()).isEmpty();
		assertThat(connection.pageInfo().hasNextPage()).isFalse();
		assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
		assertThat(connection.pageInfo().startCursor()).isNull();
		assertThat(connection.pageInfo().endCursor()).isNull();
	}
}
