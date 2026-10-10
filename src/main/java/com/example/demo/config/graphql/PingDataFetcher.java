package com.example.demo.config.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;

@DgsComponent
class PingDataFetcher {

	@PublicOperation
	@DgsQuery
	public boolean ping() {
		return true;
	}
}
