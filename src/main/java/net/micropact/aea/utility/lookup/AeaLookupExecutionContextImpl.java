/**
 *
 * Lookup Execution Context Impl
 *
 * administrator 09/15/2014
 **/

package net.micropact.aea.utility.lookup;

import com.entellitrak.ExecutionContext;
import com.entellitrak.lookup.For;
import com.entellitrak.lookup.LookupTrackingParameters;
import com.micropact.ExecutionContextImpl;
import com.micropact.entellitrak.lookup.LookupTrackingParametersBuilder;

public class AeaLookupExecutionContextImpl extends com.micropact.entellitrak.lookup.LookupExecutionContextImpl {

	public AeaLookupExecutionContextImpl (final ExecutionContext theContext,
			  							  final boolean isForTracking,
			                              final boolean isForSearch,
			                              final boolean isForView,
			                              final Long trackingId,
			                              final Long baseId,
			                              final Long parentId,
			                              final String dataObjectBusinessKey,
			                              final String dataObjectTableName) {

		super(((ExecutionContextImpl) theContext).getUserContainer(),
				isForTracking ? For.TRACKING.getType() :
					(isForSearch ? For.SEARCH.getType() :
						(isForView? For.VIEW.getType() : For.NONE.getType())),
						getLookupTrackingParameters(trackingId, baseId, parentId, dataObjectBusinessKey, dataObjectTableName)
				);
	}

	public AeaLookupExecutionContextImpl (final ExecutionContext theContext,
										  final For isForVar,
			                              final Long trackingId,
			                              final Long baseId,
			                              final Long parentId,
			                              final String dataObjectBusinessKey,
			                              final String dataObjectTableName) {
		super(((ExecutionContextImpl) theContext).getUserContainer(), isForVar.getType(),
				getLookupTrackingParameters(trackingId, baseId, parentId, dataObjectBusinessKey, dataObjectTableName));
	}

	@Override
	public boolean isFor(final String isForVar) {
		if ("liveSearch".equalsIgnoreCase(isForVar)) {
			return true;
		} else {
			return false;
		}
	}

	private static LookupTrackingParameters getLookupTrackingParameters(Long trackingId, Long baseId, Long parentId, String businessKey, String tableName){
		LookupTrackingParametersBuilder builder = new LookupTrackingParametersBuilder();
		builder.setBaseId(baseId);
		builder.setDataObjectBusinessKey(businessKey);
		builder.setDataObjectTableName(tableName);
		builder.setParentId(parentId);
		builder.setTrackingId(trackingId);
		return builder.build();
	}
}
