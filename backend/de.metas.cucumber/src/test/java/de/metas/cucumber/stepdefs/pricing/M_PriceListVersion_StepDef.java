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

package de.metas.cucumber.stepdefs.pricing;

import de.metas.cucumber.stepdefs.DataTableUtil;
import de.metas.cucumber.stepdefs.StepDefConstants;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import lombok.NonNull;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_M_PriceList_Version;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;

public class M_PriceListVersion_StepDef
{
	@And("metasfresh contains M_PriceList_Version")
	public void add_M_PriceList_Version(@NonNull final DataTable dataTable)
	{
		final List<Map<String, String>> row = dataTable.asMaps();
		for (final Map<String, String> dataTableRow : row)
		{
			createM_PriceList_Version(dataTableRow);
		}
	}

	private void createM_PriceList_Version(@NonNull final Map<String, String> row)
	{
		final String priceListVersionId = DataTableUtil.extractStringForColumnName(row, "M_PriceList_Version_ID");
		final String priceListId = DataTableUtil.extractStringForColumnName(row, "M_PriceList_ID");
		final String name = DataTableUtil.extractStringForColumnName(row, "Name");
		final String description = DataTableUtil.extractStringForColumnName(row, "Description");
		final String validFrom = DataTableUtil.extractStringForColumnName(row, "ValidFrom");
		final boolean isActive = DataTableUtil.extractBooleanForColumnName(row, "OPT.IsActive");

		final Timestamp validFromData = Timestamp.from(Instant.parse(validFrom));

		final I_M_PriceList_Version m_priceList_version = InterfaceWrapperHelper.newInstance(I_M_PriceList_Version.class);

		m_priceList_version.setM_PriceList_Version_ID(Integer.parseInt(priceListVersionId));
		m_priceList_version.setAD_Org_ID(StepDefConstants.ORG_ID.getRepoId());
		m_priceList_version.setM_PriceList_ID(Integer.parseInt(priceListId));
		m_priceList_version.setName(name);
		m_priceList_version.setDescription(description);
		m_priceList_version.setValidFrom(validFromData);
		m_priceList_version.setIsActive(isActive);

		saveRecord(m_priceList_version);
	}
}
