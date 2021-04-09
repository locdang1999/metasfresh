/*
 * #%L
 * de.metas.cucumber
 * %%
 * Copyright (C) 2021 metas GmbH
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

package de.metas.cucumber.stepdefs.externalreference;

import de.metas.common.externalreference.JsonExternalReferenceItem;
import de.metas.common.externalreference.JsonExternalReferenceLookupItem;
import de.metas.common.externalreference.JsonExternalReferenceLookupRequest;
import de.metas.common.externalreference.JsonExternalReferenceLookupResponse;
import de.metas.common.externalsystem.JsonExternalSystemName;
import de.metas.common.rest_api.common.JsonMetasfreshId;
import de.metas.cucumber.stepdefs.DataTableUtil;
import de.metas.externalreference.ExternalIdentifier;
import de.metas.externalreference.IExternalReferenceType;
import de.metas.externalreference.model.I_S_ExternalReference;
import de.metas.externalreference.rest.ExternalReferenceRestControllerService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import lombok.NonNull;
import org.adempiere.ad.persistence.modelgen.TableAndColumnInfoRepository;
import org.adempiere.ad.persistence.modelgen.TableInfo;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.SpringContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class S_ExternalReference_StepDef
{
	private final TableAndColumnInfoRepository repository;
	private final ExternalReferenceRestControllerService externalReferenceRestControllerService;

	public S_ExternalReference_StepDef(final TableAndColumnInfoRepository repository)
	{
		this.repository = repository;
		this.externalReferenceRestControllerService = SpringContextHolder.instance.getBean(ExternalReferenceRestControllerService.class);
	}

	@And("metasfresh contains S_ExternalReference for M_Product")
	public void add_S_ExternalReference_for_M_Product(@NonNull final DataTable dataTable)
	{
		final List<Map<String, String>> row = dataTable.asMaps();
		for (final Map<String, String> dataTableRow : row)
		{
			final String externalSystem = DataTableUtil.extractStringForColumnName(dataTableRow, "ExternalSystem");
			final String externalReference = DataTableUtil.extractStringForColumnName(dataTableRow, "ExternalReference");
			final String type = DataTableUtil.extractStringForColumnName(dataTableRow, "Type");
			final int recordId = DataTableUtil.extractIntForColumnName(dataTableRow, "Record_ID");
			final String tableName = DataTableUtil.extractStringForColumnName(dataTableRow, "TableName");
			final int orgId = DataTableUtil.extractIntForColumnName(dataTableRow, "Org_ID");

			final TableInfo tableInfo = repository.getTableInfo(tableName);

			final I_S_ExternalReference s_externalReference = InterfaceWrapperHelper.newInstance(I_S_ExternalReference.class);
			s_externalReference.setExternalSystem(externalSystem);
			s_externalReference.setExternalReference(externalReference);
			s_externalReference.setType(type);
			s_externalReference.setRecord_ID(recordId);
			s_externalReference.setReferenced_AD_Table_ID(tableInfo.getAdTableId());
			s_externalReference.setAD_Org_ID(orgId);
			InterfaceWrapperHelper.saveRecord(s_externalReference);
		}
	}

	public Optional<JsonMetasfreshId> getJsonMetasfreshIdFromExternalReference(
			@NonNull final ExternalIdentifier externalIdentifier,
			@NonNull final IExternalReferenceType type)
	{
		final String orgCode = null;
		final JsonExternalReferenceLookupResponse lookupResponse = externalReferenceRestControllerService.performLookup(
				orgCode,
				JsonExternalReferenceLookupRequest.builder()
						.systemName(JsonExternalSystemName.of(externalIdentifier.asExternalValueAndSystem().getExternalSystem()))
						.item(JsonExternalReferenceLookupItem.builder()
									  .type(type.getCode())
									  .id(externalIdentifier.asExternalValueAndSystem().getValue())
									  .build())
						.build());
		return lookupResponse.getItems()
				.stream()
				.map(JsonExternalReferenceItem::getMetasfreshId)
				.filter(Objects::nonNull)
				.findFirst();
	}
}
