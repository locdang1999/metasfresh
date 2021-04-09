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

package de.metas.cucumber.stepdefs.product;

import de.metas.cucumber.stepdefs.DataTableUtil;
import de.metas.cucumber.stepdefs.StepDefConstants;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import lombok.NonNull;
import org.compiere.model.I_M_ProductPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;

public class M_ProductPrice_StepDef
{
	@And("metasfresh contains M_ProductPrice")
	public void add_M_ProductPrice(@NonNull final DataTable dataTable)
	{
		final List<Map<String, String>> productPriceTableList = dataTable.asMaps();
		for (final Map<String, String> dataTableRow : productPriceTableList)
		{
			final int productId = DataTableUtil.extractIntForColumnName(dataTableRow, "M_Product_ID");
			final int productPriceId = DataTableUtil.extractIntForColumnName(dataTableRow, "M_ProductPrice_ID");
			final int priceListVersionId = DataTableUtil.extractIntForColumnName(dataTableRow, "M_PriceList_Version_ID");
			final BigDecimal priceStd = DataTableUtil.extractBigDecimalForColumnName(dataTableRow, "PriceStd");
			final int seqNo = DataTableUtil.extractIntForColumnName(dataTableRow, "SeqNo");
			final boolean isActive = DataTableUtil.extractBooleanForColumnName(dataTableRow, "OPT.IsActive");

			final I_M_ProductPrice mockedProductPrice = newInstance(I_M_ProductPrice.class);
			mockedProductPrice.setM_Product_ID(productId);
			mockedProductPrice.setM_ProductPrice_ID(productPriceId);
			mockedProductPrice.setM_PriceList_Version_ID(priceListVersionId);
			mockedProductPrice.setPriceStd(priceStd);
			mockedProductPrice.setSeqNo(seqNo);
			mockedProductPrice.setIsActive(isActive);
			mockedProductPrice.setC_TaxCategory_ID(StepDefConstants.TAX_CATEGORY_ID_NORMAL.getRepoId());
			mockedProductPrice.setC_UOM_ID(StepDefConstants.PCE_UOM_ID.getRepoId());
			saveRecord(mockedProductPrice);
		}
	}
}
