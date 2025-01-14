/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2020 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package de.metas.ui.web.window.model.lookup;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import de.metas.adempiere.service.impl.TooltipType;
import de.metas.cache.CCache.CCacheStats;
import de.metas.i18n.ITranslatableString;
import de.metas.i18n.TranslatableStrings;
import de.metas.logging.LogManager;
import de.metas.ui.web.window.WindowConstants;
import de.metas.ui.web.window.datatypes.DebugProperties;
import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.IntegerLookupValue;
import de.metas.ui.web.window.datatypes.LookupValue.StringLookupValue;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.datatypes.LookupValuesPage;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.descriptor.LookupDescriptor;
import de.metas.ui.web.window.descriptor.sql.SqlForFetchingLookupById;
import de.metas.ui.web.window.descriptor.sql.SqlForFetchingLookups;
import de.metas.ui.web.window.descriptor.sql.SqlLookupDescriptor;
import de.metas.util.StringUtils;
import lombok.NonNull;
import org.adempiere.ad.service.impl.LookupDAO.SQLNamePairIterator;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.validationRule.INamePairPredicate;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;
import org.compiere.util.NamePair;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GenericSqlLookupDataSourceFetcher implements LookupDataSourceFetcher
{
	@NonNull
	private final TooltipType tooltipType;

	public static GenericSqlLookupDataSourceFetcher of(final LookupDescriptor lookupDescriptor)
	{
		return new GenericSqlLookupDataSourceFetcher(lookupDescriptor);
	}

	private static final Logger logger = LogManager.getLogger(GenericSqlLookupDataSourceFetcher.class);

	private final @NonNull String lookupTableName;
	private final @NonNull Optional<String> lookupTableNameAsOptional;
	private final boolean numericKey;
	private final int entityTypeIndex;

	private final SqlForFetchingLookups sqlForFetchingExpression;
	private final SqlForFetchingLookupById sqlForFetchingLookupByIdExpression;
	private final INamePairPredicate postQueryPredicate;

	private final boolean isTranslatable;

	private final Optional<WindowId> zoomIntoWindowId;

	private GenericSqlLookupDataSourceFetcher(@NonNull final LookupDescriptor lookupDescriptor)
	{
		final SqlLookupDescriptor sqlLookupDescriptor = lookupDescriptor.cast(SqlLookupDescriptor.class);

		lookupTableNameAsOptional = sqlLookupDescriptor.getTableName();
		lookupTableName = lookupTableNameAsOptional.orElseThrow(() -> new AdempiereException("No table name defined for " + lookupDescriptor));
		numericKey = sqlLookupDescriptor.isNumericKey();
		entityTypeIndex = sqlLookupDescriptor.getEntityTypeIndex();
		sqlForFetchingExpression = sqlLookupDescriptor.getSqlForFetchingExpression();
		sqlForFetchingLookupByIdExpression = sqlLookupDescriptor.getSqlForFetchingLookupByIdExpression();
		postQueryPredicate = sqlLookupDescriptor.getPostQueryPredicate();

		isTranslatable = sqlForFetchingLookupByIdExpression.requiresParameter(LookupDataSourceContext.PARAM_AD_Language.getName());

		zoomIntoWindowId = lookupDescriptor.getZoomIntoWindowId();

		tooltipType = sqlLookupDescriptor.getTooltipType();
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("lookupTableName", lookupTableName)
				.add("sqlForFetchingExpression", sqlForFetchingExpression)
				.add("postQueryPredicate", postQueryPredicate)
				.toString();
	}

	@Override
	public String getCachePrefix()
	{
		// NOTE: it's very important to have the lookupTableName as cache name prefix because we want the cache invalidation to happen for this table
		return lookupTableName;
	}

	@Override
	public Optional<String> getLookupTableName()
	{
		return lookupTableNameAsOptional;
	}

	@Override
	public Optional<WindowId> getZoomIntoWindowId()
	{
		return zoomIntoWindowId;
	}

	@Override
	public boolean isCached()
	{
		return false;
	}

	@Override
	public void cacheInvalidate()
	{
	}

	@Override
	public List<CCacheStats> getCacheStats()
	{
		return ImmutableList.of();
	}

	@Override
	public final LookupDataSourceContext.Builder newContextForFetchingById(final Object id)
	{
		return LookupDataSourceContext.builder(lookupTableName)
				.putFilterByIdParameterName("?")
				.putFilterById(id)
				.setRequiredParameters(sqlForFetchingLookupByIdExpression.getParameters());
	}

	@Override
	public LookupDataSourceContext.Builder newContextForFetchingList()
	{
		return LookupDataSourceContext.builder(lookupTableName)
				.putPostQueryPredicate(postQueryPredicate)
				.setRequiredParameters(sqlForFetchingExpression.getParameters());
	}

	@Override
	public final boolean isNumericKey()
	{
		return numericKey;
	}

	/**
	 * @return lookup values list
	 */
	@Override
	public LookupValuesPage retrieveEntities(final LookupDataSourceContext evalCtxParam)
	{
		final int offset = evalCtxParam.getOffset(0);
		final int pageLength = evalCtxParam.getLimit(1000);
		final LookupDataSourceContext evalCtxEffective = evalCtxParam
				.withOffset(offset)
				.withLimit(pageLength < Integer.MAX_VALUE ? pageLength + 1 : pageLength); // add 1 to pageLength to be able to recognize if there are more items

		final String sqlForFetching = sqlForFetchingExpression.evaluate(evalCtxEffective);
		final String adLanguage = isTranslatable ? evalCtxEffective.getAD_Language() : null;

		try (final SQLNamePairIterator data = new SQLNamePairIterator(sqlForFetching, numericKey, entityTypeIndex))
		{
			DebugProperties debugProperties = null;
			if (WindowConstants.isProtocolDebugging())
			{
				debugProperties = DebugProperties.EMPTY
						.withProperty("debug-sql", sqlForFetching)
						.withProperty("debug-params", evalCtxEffective.toString());
			}

			final List<NamePair> valuesBeforePostFilter = data.fetchAll();
			final boolean hasMoreResults = valuesBeforePostFilter.size() > pageLength;

			final LookupValuesList values = valuesBeforePostFilter
					.stream()
					.limit(pageLength)
					.filter(evalCtxEffective::acceptItem)
					.map(namePair -> LookupValue.fromNamePair(namePair, adLanguage, this.tooltipType))
					.collect(LookupValuesList.collect(debugProperties));

			logger.trace("Returning values={} (executed sql: {})", values, sqlForFetching);
			return LookupValuesPage.ofValuesAndHasMoreFlag(values, hasMoreResults);
		}
	}

	@Override
	public final LookupValue retrieveLookupValueById(final @NonNull LookupDataSourceContext evalCtx)
	{
		final Object id = evalCtx.getIdToFilter();
		if (id == null)
		{
			throw new IllegalStateException("No ID provided in " + evalCtx);
		}

		final String sqlForFetchingLookupById = sqlForFetchingLookupByIdExpression.evaluate(evalCtx);

		final String[] nameAndDescriptionAndActive = DB.getSQLValueArrayEx(ITrx.TRXNAME_None, sqlForFetchingLookupById, id);
		if (nameAndDescriptionAndActive == null || nameAndDescriptionAndActive.length == 0)
		{
			return LOOKUPVALUE_NULL;
		}

		final String displayName = nameAndDescriptionAndActive[0];
		final String description = nameAndDescriptionAndActive.length >= 2 ? nameAndDescriptionAndActive[1] : null;
		final boolean active = nameAndDescriptionAndActive.length < 3 || StringUtils.toBoolean(nameAndDescriptionAndActive[2]);

		final ITranslatableString displayNameTrl;
		final ITranslatableString descriptionTrl;
		if (isTranslatable)
		{
			final String adLanguage = evalCtx.getAD_Language();
			displayNameTrl = TranslatableStrings.singleLanguage(adLanguage, displayName);
			descriptionTrl = TranslatableStrings.singleLanguage(adLanguage, description);
		}
		else
		{
			displayNameTrl = TranslatableStrings.anyLanguage(displayName);
			descriptionTrl = TranslatableStrings.anyLanguage(description);
		}

		if (id instanceof Integer)
		{
			final Integer idInt = (Integer)id;
			return IntegerLookupValue.builder()
					.id(idInt)
					.displayName(displayNameTrl)
					.description(descriptionTrl)
					.active(active)
					.build();
		}
		else
		{
			final String idString = id.toString();
			return StringLookupValue.builder()
					.id(idString)
					.displayName(displayNameTrl)
					.description(descriptionTrl)
					.active(active)
					.build();
		}
	}
}
