package de.metas.pricing.service;

import de.metas.currency.CurrencyPrecision;
import de.metas.lang.SOTrx;
import de.metas.location.CountryId;
import de.metas.pricing.PriceListId;
import de.metas.pricing.PricingSystemId;
import de.metas.util.ISingletonService;
import lombok.NonNull;
import org.compiere.model.I_M_PriceList;
import org.compiere.model.I_M_PriceList_Version;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * @author RC
 */
public interface IPriceListBL extends ISingletonService
{
	CurrencyPrecision getPricePrecision(@NonNull PriceListId priceListId);

	CurrencyPrecision getAmountPrecision(@NonNull PriceListId priceListId);

	CurrencyPrecision getTaxPrecision(@NonNull PriceListId priceListId);

	/**
	 * @return the current price list for vendor if any (for the giver pricing system), null otherwise
	 */
	I_M_PriceList getCurrentPricelistOrNull(
			PricingSystemId pricingSystemId,
			CountryId countryId,
			ZonedDateTime date,
			SOTrx soTrx);

	/**
	 * Find the current version from a pricing system based on the given parameters.
	 *
	 * @param soTrx                 SO/PO or null
	 * @param processedPLVFiltering if not <code>null</code>, then only PLVs which have the give value in their <code>Processed</code> column are considered.
	 *                              task 09533: the user doesn't know about PLV's processed flag, so in most cases we can't filter by it
	 */
	@Nullable
	I_M_PriceList_Version getCurrentPriceListVersionOrNull(
			@Nullable PricingSystemId pricingSystemId,
			CountryId countryId,
			ZonedDateTime date,
			@Nullable SOTrx soTrx,
			Boolean processedPLVFiltering);

	/**
	 * Please keep in sync with {@link #updateAllPLVName(String, PriceListId)} )}
	 */
	String createPLVName(final @NonNull String priceListName, @NonNull LocalDate date);

	/**
	 * Update the Name of all Price List Versions.
	 * <p>
	 * The name has the format "Price List.Name + PLV.ValidFrom".
	 * <p>
	 * Please keep in sync with {@link #createPLVName(String, LocalDate)}
	 *
	 * @return number of updated records
	 */
	int updateAllPLVName(final String namePrefix, final PriceListId priceListId);
}