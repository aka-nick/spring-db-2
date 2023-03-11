package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.StringUtils;

/**
 * SimpleJdbcInsert 기능 추가 - 쉬운 인서트 기능 사용
 */
@Slf4j
public class JdbcTemplateItemRepositoryV3 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;
    private final SimpleJdbcInsert jdbcInsert;

    public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("item")
                .usingGeneratedKeyColumns("id"); // pk 컬럼 지정
//                .usingColumns("item_name", "price", "quantity");
                // dataSource를 통해 메타데이터 조회하여 알아서 컬럼 인식할 수 있음. 고로 생략 가능
    }

    @Override
    public Item save(Item item) {
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        Number key = jdbcInsert.executeAndReturnKey(param);
        item.setId(key.longValue());
//        String sql = "insert into item(item_name, price, quantity) "
//                + "values (:item_name, :price, :quantity)";

//        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//        template.update(sql, param, keyHolder);
//        long key = keyHolder.getKey().longValue();
//        item.setId(key);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=:item_name, price=:price, quantity=:quantity"
                + " where id=:id ";
        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("item_name", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";
        try {
//            Item item = template.queryForObject(sql, itemRowMapper(), id);
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        }
        catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
        catch (IncorrectResultSizeDataAccessException e) {
            return Optional.empty(); //조회 데이터가 하나여야 하는데 두 개 이상일 때
        }
    }
    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item ";

        // 동적쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }
        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }

        log.info("sql={}", sql);

        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class); // camelCase 변환 지원
    }
}
