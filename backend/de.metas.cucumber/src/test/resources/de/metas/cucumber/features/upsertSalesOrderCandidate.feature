Feature: Upsert sales order candidate

  Background:
    Given the existing user with login 'metasfresh' receives a random a API token for the existing role with name 'WebUI'

  Scenario: Create sales order candidate
    Given metasfresh contains external bpartnerIdentifier C_BPartner
      | BPartnerIdentifier | Name           |
      | ext-ALBERTA-12345  | bpartner_12345 |
    And metasfresh contains C_BPartner_location
      | BPartnerIdentifier | LocationIdentifier | CountryCode |
      | ext-ALBERTA-12345  | gln-l12345         | DE          |
    And metasfresh contains M_Product with M_Product_ID '12345'
    And metasfresh contains S_ExternalReference for M_Product
      | ExternalSystem | ExternalReference | Type    | Record_ID | TableName | Org_ID  |
      | ALBERTA        | 12345             | Product | 12345     | M_Product | 1000000 |
    And metasfresh contains M_PricingSystem
      | M_PricingSystem_ID | Name                  | Value                 | Description           | IsActive |
      | 123001             | pricing_system_123001 | pricing_system_123001 | pricing_system_123001 | true     |
    And metasfresh contains M_PriceList
      | M_PriceList_ID | M_PricingSystem_ID | OPT.C_Country.CountryCode | C_Currency.ISO_Code | Name                   | OPT.Description | SOTrx | IsTaxIncluded | PricePrecision | IsActive |
      | 123002         | 123001             | DE                        | CHF                 | price_list_name_123002 | null            | true  | false         | 2              | true     |
    And metasfresh contains M_PriceList_Version
      | M_PriceList_Version_ID | M_PriceList_ID | Name        | Description | ValidFrom            | OPT.IsActive |
      | 123003                 | 123002         | test_123003 | test_123003 | 2020-09-03T00:00:00Z | true         |
    And metasfresh contains M_ProductPrice
      | M_Product_ID | M_ProductPrice_ID | M_PriceList_Version_ID | PriceStd | SeqNo | OPT.IsActive |
      | 12345        | 123004            | 123003                 | 22       | 10    | true         |
    When a 'POST' request with the below payload is sent to the metasfresh REST-API '/api/v2-pre/orders/sales/candidates' and fulfills with '201' status code
    """
{
  "bpartner": {
    "bpartnerIdentifier": "ext-ALBERTA-12345",
    "bpartnerLocationIdentifier": "gln-l12345"
  },
  "dataSource": "int-SOURCE.de.metas.rest_api.ordercandidates.impl.OrderCandidatesRestControllerImpl",
  "dateRequired": "2021-04-03",
  "externalHeaderId": "ExternalHeaderId_12345",
  "externalLineId": "ExternalLineId_12345",
  "orgCode": "001",
  "poReference": "poReference_12345",
  "productIdentifier": "ext-ALBERTA-12345",
  "qty": 20,
  "pricingSystemCode": "pricing_system_123001",
  "uomCode": "PCE"
}
"""
    Then verify that order candidates data was created
      | BPartnerIdentifier | BPartnerLocationIdentifier | DateRequired | ExternalHeaderId       | ExternalLineId       | OrgCode | PoReference       | ProductIdentifier | Qty | PricingSystemCode     | UomCode |
      | ext-ALBERTA-12345  | gln-l12345                 | 2021-04-03   | ExternalHeaderId_12345 | ExternalLineId_12345 | 001     | poReference_12345 | ext-ALBERTA-12345 | 20  | pricing_system_123001 | PCE     |