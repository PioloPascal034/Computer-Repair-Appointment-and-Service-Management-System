package crsm;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Marketplace links are opened only by the user's explicit click; they are not scraped. */
public final class SupplierLinks {
    private SupplierLinks() {}
    public static String shopeeSearch(Map<String,Object> part){
        String query=Stream.of("brand","model","part_name","specification").map(key->part.get(key)).filter(value->value!=null&&!value.toString().isBlank()).map(Object::toString).collect(Collectors.joining(" "));
        if(query.isBlank())throw new IllegalArgumentException("Select a part to search for.");
        return "https://shopee.ph/search?keyword="+URLEncoder.encode(query,StandardCharsets.UTF_8);
    }
}
