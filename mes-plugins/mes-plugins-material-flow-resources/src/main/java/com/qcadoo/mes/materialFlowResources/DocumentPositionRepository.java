package com.qcadoo.mes.materialFlowResources;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.PalletNumberDTO;
import com.qcadoo.mes.materialFlowResources.mappers.DocumentPositionMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentPositionRepository {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public List<DocumentPositionDTO> findAll(final Long documentId, final String sidx, final String sord) {
        String query = "SELECT position.*, product.number as product_number, additionalcode.code as additionalcode_code, palletnumber.number as palletnumber_number, location.number as storagelocation_number\n"
                + "	FROM materialflowresources_position position\n"
                + "	left join basic_product product on (position.product_id = product.id)\n"
                + "	left join basic_additionalcode additionalcode on (position.additionalcode_id = additionalcode.id)\n"
                + "	left join basic_palletnumber palletnumber on (position.palletnumber_id = palletnumber.id)\n"
                + "	left join materialflowresources_storagelocation location on (position.storagelocation_id = location.id) WHERE position.document_id = :documentId ORDER BY " + sidx + " " + sord;

        List<DocumentPositionDTO> list = jdbcTemplate.query(query, Collections.singletonMap("documentId", documentId), new DocumentPositionMapper());

        return list;
    }

    public void delete(Long id) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("DELETE FROM materialflowresources_position WHERE id = :id ");
        jdbcTemplate.update(queryBuilder.toString(), Collections.singletonMap("id", id));
    }

    public void create(DocumentPositionDTO documentPositionVO) {
        Map<String, Object> params = tryMapDocumentPositionVOToParams(documentPositionVO);
        params.remove("id");

        String keys = params.keySet().stream().collect(Collectors.joining(", "));
        String values = params.keySet().stream().map(key -> {
            return ":" + key;
        }).collect(Collectors.joining(", "));

        String query = String.format("INSERT INTO materialflowresources_position (%s) VALUES (%s) RETURNING id", keys, values);

        jdbcTemplate.queryForObject(query, params, Long.class);
    }

    public void update(Long id, DocumentPositionDTO documentPositionVO) {
        Map<String, Object> params = tryMapDocumentPositionVOToParams(documentPositionVO);

        String set = params.keySet().stream().map(key -> {
            return key + "=:" + key;
        }).collect(Collectors.joining(", "));
        String query = String.format("UPDATE materialflowresources_position SET %s WHERE id = :id ", set);

        jdbcTemplate.update(query, params);
    }

    private Map<String, Object> tryMapDocumentPositionVOToParams(DocumentPositionDTO vo) {
        Map<String, Object> params = new HashMap<>();

        params.put("id", vo.getId());
        params.put("product_id", tryGetProductIdByNumber(vo.getProduct()));
        params.put("additionalcode_id", tryGetAdditionalCodeIdByCode(vo.getAdditional_code()));
        params.put("quantity", vo.getQuantity());
        params.put("givenquantity", vo.getGivenquantity());
        params.put("givenunit", vo.getGivenunit());
        params.put("conversion", vo.getConversion());
        params.put("expirationdate", vo.getExpirationdate());
        params.put("palletnumber_id", tryGetPalletNumberIdByNumber(vo.getPallet()));
        params.put("typeofpallet", vo.getType_of_pallet());
        params.put("storagelocation_id", tryGetStorageLocationIdByNumber(vo.getStorage_location()));
        params.put("document_id", vo.getDocument());

        return params;
    }

    private Long tryGetProductIdByNumber(String productNumber) {
        if (Strings.isNullOrEmpty(productNumber)) {
            return null;
        }

        try {
            Long productId = jdbcTemplate.queryForObject("SELECT product.id FROM basic_product product WHERE product.number = :number", Collections.singletonMap("number", productNumber), Long.class);

            return productId;

        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException(String.format("Nie znaleziono takiego produktu: '%s'.", productNumber));
        }
    }

    private Long tryGetAdditionalCodeIdByCode(String additionalCode) {
        if (Strings.isNullOrEmpty(additionalCode)) {
            return null;
        }

        try {
            Long additionalCodeId = jdbcTemplate.queryForObject("SELECT additionalcode.id FROM basic_additionalcode additionalcode WHERE additionalcode.code = :code",
                    Collections.singletonMap("code", additionalCode), Long.class);

            return additionalCodeId;

        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException(String.format("Nie znaleziono takiego dodatkowego kodu: '%s'.", additionalCode));
        }
    }

    private Long tryGetPalletNumberIdByNumber(String palletNumber) {
        if (Strings.isNullOrEmpty(palletNumber)) {
            return null;
        }

        try {
            Long palletNumberId = jdbcTemplate.queryForObject("SELECT palletnumber.id FROM basic_palletnumber palletnumber WHERE palletnumber.number = :number",
                    Collections.singletonMap("number", palletNumber), Long.class);

            return palletNumberId;

        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException(String.format("Nie znaleziono takiego numeru palety: '%s'.", palletNumber));
        }
    }

    private Long tryGetStorageLocationIdByNumber(String storageLocationNumber) {
        if (Strings.isNullOrEmpty(storageLocationNumber)) {
            return null;
        }

        try {
            Long storageLocationId = jdbcTemplate.queryForObject("SELECT storagelocation.id FROM materialflowresources_storagelocation storagelocation WHERE storagelocation.number = :number",
                    Collections.singletonMap("number", storageLocationNumber), Long.class);

            return storageLocationId;

        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException(String.format("Nie znaleziono takiego miejsca składowania: '%s'.", storageLocationNumber));
        }
    }

    public List<StorageLocationDTO> getStorageLocations(String q) {
        if (Strings.isNullOrEmpty(q)) {
            return Lists.newArrayList();

        } else {
            String query = "SELECT id, number from materialflowresources_storagelocation WHERE number ilike :q LIMIT 15;";
            return jdbcTemplate.query(query, Collections.singletonMap("q", '%' + q + '%'), new BeanPropertyRowMapper(StorageLocationDTO.class));
        }
    }

    public Map<String, String> getGridConfig() {
        String query = "select showstoragelocation from materialflowresources_documentpositionparameters";
        return jdbcTemplate.queryForMap(query, Collections.EMPTY_MAP);
    }
}
