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

package de.metas.cucumber.stepdefs.salesordercandidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.metas.common.bpartner.v2.response.JsonResponseBPartner;
import de.metas.common.bpartner.v2.response.JsonResponseLocation;
import de.metas.common.ordercandidates.v2.response.JsonOLCand;
import de.metas.common.ordercandidates.v2.response.JsonOLCandCreateBulkResponse;
import de.metas.common.ordercandidates.v2.response.JsonResponseBPartnerLocationAndContact;
import de.metas.common.rest_api.common.JsonMetasfreshId;
import de.metas.cucumber.stepdefs.DataTableUtil;
import de.metas.cucumber.stepdefs.context.TestContext;
import de.metas.cucumber.stepdefs.externalreference.S_ExternalReference_StepDef;
import de.metas.externalreference.ExternalIdentifier;
import de.metas.externalreference.bpartner.BPartnerExternalReferenceType;
import de.metas.externalreference.product.ProductExternalReferenceType;
import de.metas.pricing.PricingSystemId;
import de.metas.pricing.service.IPriceListDAO;
import de.metas.uom.IUOMDAO;
import de.metas.uom.UomId;
import de.metas.uom.X12DE355;
import de.metas.util.Services;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import lombok.NonNull;
import org.adempiere.exceptions.AdempiereException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class UpsertSalesOrderCandidate_StepDef
{
	private final TestContext testContext;
	private final S_ExternalReference_StepDef s_externalReference_stepDef;

	private final IUOMDAO uomDAO = Services.get(IUOMDAO.class);
	private final IPriceListDAO priceListsDAO = Services.get(IPriceListDAO.class);
	private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
			.registerModule(new JavaTimeModule());

	public UpsertSalesOrderCandidate_StepDef(@NonNull final TestContext testContext, final S_ExternalReference_StepDef s_externalReference_stepDef)
	{
		this.testContext = testContext;
		this.s_externalReference_stepDef = s_externalReference_stepDef;
	}

	@Then("verify that order candidates data was created")
	public void validateCandidateOrderData(@NonNull final DataTable dataTable) throws JsonProcessingException
	{
		final String responseJson = testContext.getApiResponse().getContent();
		final JsonOLCandCreateBulkResponse response = objectMapper.readValue(responseJson, JsonOLCandCreateBulkResponse.class);
		final List<JsonOLCand> jsonOLCands = response.getResult();

		final List<Map<String, String>> orderCandidateDataRequest = dataTable.asMaps();

		for (final Map<String, String> row : orderCandidateDataRequest)
		{
			final String externalHeaderId = DataTableUtil.extractStringForColumnName(row, "ExternalHeaderId");
			final String externalLineId = DataTableUtil.extractStringForColumnName(row, "ExternalLineId");

			final JsonOLCand jsonOLCandResponse = jsonOLCands.stream()
					.filter(item -> externalHeaderId.equals(item.getExternalHeaderId()) && externalLineId.equals(item.getExternalLineId()))
					.findFirst()
					.orElseThrow(() -> new AdempiereException("No order candidate with externalHeaderId " + externalHeaderId + "and externalLineId " + externalLineId + " found in response"));

			validateSalesOrderCandidateData(row, jsonOLCandResponse);
		}
	}

	private void validateSalesOrderCandidateData(
			@NonNull final Map<String, String> row,
			@NonNull final JsonOLCand response)
	{
		final String bpartnerIdentifier = DataTableUtil.extractStringForColumnName(row, "BPartnerIdentifier");
		final String bpartnerLocationIdentifier = DataTableUtil.extractStringForColumnName(row, "BPartnerLocationIdentifier");
		final LocalDate dateRequired = DataTableUtil.extractLocalDateOrNullForColumnName(row, "DateRequired");
		final String externalHeaderId = DataTableUtil.extractStringForColumnName(row, "ExternalHeaderId");
		final String externalLineId = DataTableUtil.extractStringForColumnName(row, "ExternalLineId");
		final String orgCode = DataTableUtil.extractStringForColumnName(row, "OrgCode");
		final String poReference = DataTableUtil.extractStringForColumnName(row, "PoReference");
		final String productIdentifier = DataTableUtil.extractStringForColumnName(row, "ProductIdentifier");
		final BigDecimal qty = DataTableUtil.extractBigDecimalForColumnName(row, "Qty");
		final String pricingSystemCode = DataTableUtil.extractStringForColumnName(row, "PricingSystemCode");
		final String uomCode = DataTableUtil.extractStringForColumnName(row, "UomCode");

		final UomId uomId = uomDAO.getUomIdByX12DE355(X12DE355.ofCode(uomCode));
		final PricingSystemId pricingSystemId = priceListsDAO.getPricingSystemIdByValue(pricingSystemCode);

		assertThat(response.getExternalHeaderId()).isEqualTo(externalHeaderId);
		assertThat(response.getExternalLineId()).isEqualTo(externalLineId);
		assertThat(response.getPoReference()).isEqualTo(poReference);
		assertThat(response.getQty()).isEqualTo(qty);
		assertThat(response.getOrgCode()).isEqualTo(orgCode);
		assertThat(response.getDatePromised()).isEqualTo(dateRequired);
		assertThat(response.getUomId()).isEqualTo(uomId.getRepoId());
		assertThat(response.getPricingSystemId()).isEqualTo(pricingSystemId.getRepoId());

		final ExternalIdentifier productExternalIdentifier = ExternalIdentifier.of(productIdentifier);
		final Optional<JsonMetasfreshId> productMetasfreshId = s_externalReference_stepDef
				.getJsonMetasfreshIdFromExternalReference(productExternalIdentifier, ProductExternalReferenceType.PRODUCT);
		assertThat(response.getProductId()).isEqualTo(productMetasfreshId.get().getValue());

		final JsonResponseBPartnerLocationAndContact bPartnerLocationAndContactResponse = response.getBpartner();

		final JsonResponseBPartner bpartnerResponse = bPartnerLocationAndContactResponse.getBpartner();
		final ExternalIdentifier bpartnerExternalIdentifier = ExternalIdentifier.of(bpartnerIdentifier);
		final Optional<JsonMetasfreshId> bpartnerMetasfreshId = s_externalReference_stepDef
				.getJsonMetasfreshIdFromExternalReference(bpartnerExternalIdentifier, BPartnerExternalReferenceType.BPARTNER);
		assertThat(bpartnerResponse.getMetasfreshId().getValue()).isEqualTo(bpartnerMetasfreshId.get().getValue());

		final JsonResponseLocation jsonResponseLocation = bPartnerLocationAndContactResponse.getLocation();
		final ExternalIdentifier bpartnerLocationExternalIdentifier = ExternalIdentifier.of(bpartnerLocationIdentifier);
		final String bpartnerLocationGLNCode = bpartnerLocationExternalIdentifier.asGLN().getCode();
		assertThat(jsonResponseLocation.getGln()).isEqualTo(bpartnerLocationGLNCode);
	}
}