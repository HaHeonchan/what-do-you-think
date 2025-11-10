package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleCustomSearchService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${google.custom-search.api-key:}")
    private String apiKey;
    
    @Value("${google.custom-search.cx-id:}")
    private String cxId;
    
    @Value("${google.custom-search.base-url:https://www.googleapis.com/customsearch/v1}")
    private String baseUrl;
    
    @Value("${google.custom-search.excluded-domains:}")
    private String excludedDomains;
    
    private List<String> excludedDomainsList;
    
    public GoogleCustomSearchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        // 제외할 도메인 목록 초기화
        excludedDomainsList = new ArrayList<>();
        if (excludedDomains != null && !excludedDomains.trim().isEmpty()) {
            String[] domains = excludedDomains.split(",");
            for (String domain : domains) {
                String trimmed = domain.trim();
                if (!trimmed.isEmpty()) {
                    excludedDomainsList.add(trimmed.toLowerCase());
                }
            }
        }
        System.out.println("[GoogleCustomSearchService] 제외할 도메인 수: " + excludedDomainsList.size());
    }
    
    /**
     * Google Custom Search API를 사용하여 웹 검색을 수행합니다.
     * 
     * @param query 검색할 쿼리 문자열
     * @param numResults 반환할 결과 수 (기본값: 5, 최대: 10)
     * @return 검색 결과 리스트 (제목, 링크, 스니펫 포함)
     */
    public List<SearchResult> search(String query, int numResults) {
        System.out.println("\n========== [웹 검색 시작] ==========");
        System.out.println("[검색 쿼리] " + query);
        System.out.println("[요청 결과 수] " + numResults);
        
        if (query == null || query.trim().isEmpty()) {
            System.out.println("[검색 결과] 쿼리가 비어있어 검색을 건너뜁니다.");
            System.out.println("=====================================\n");
            return new ArrayList<>();
        }
        
        // API 키와 CX ID가 설정되지 않은 경우 빈 결과 반환
        if (apiKey == null || apiKey.trim().isEmpty() || cxId == null || cxId.trim().isEmpty()) {
            System.err.println("[검색 실패] Google Custom Search API 키 또는 CX ID가 설정되지 않았습니다.");
            System.out.println("=====================================\n");
            return new ArrayList<>();
        }
        
        // 결과 수 제한 (1-10)
        int safeNumResults = Math.max(1, Math.min(10, numResults));
        System.out.println("[실제 요청 결과 수] " + safeNumResults);
        
        try {
            // API 요청 URL 구성
            // 원본 검색어를 그대로 사용 (인코딩 없이)
            String url = baseUrl + "?key=" + apiKey + "&cx=" + cxId + "&q=" + query + "&num=" + safeNumResults;
            
            System.out.println("[API 호출] Google Custom Search API 요청 중...");
            System.out.println("[원본 검색어 (인코딩 없이)] " + query);
            System.out.println("[요청 URL] " + url.replace("key=" + apiKey, "key=***").replace("cx=" + cxId, "cx=***"));
            
            // API 호출
            String response = restTemplate.getForObject(url, String.class);
            
            System.out.println("[응답 수신] 응답 길이: " + (response != null ? response.length() : 0) + "자");
            
            if (response == null || response.isEmpty()) {
                System.err.println("[검색 실패] Google Custom Search API 응답이 비어있습니다.");
                System.out.println("=====================================\n");
                return new ArrayList<>();
            }
            
            // 응답 원본 전체 출력
            System.out.println("[응답 원본 JSON 전체]");
            System.out.println(response);
            System.out.println();
            
            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(response);
            
            // 응답 구조 분석
            System.out.println("[응답 구조 분석]");
            if (rootNode.has("error")) {
                JsonNode error = rootNode.get("error");
                System.err.println("[API 오류] " + error.toString());
            }
            if (rootNode.has("searchInformation")) {
                JsonNode searchInfo = rootNode.get("searchInformation");
                System.out.println("  - 검색 시간: " + (searchInfo.has("searchTime") ? searchInfo.get("searchTime").asText() : "N/A"));
                System.out.println("  - 총 결과 수: " + (searchInfo.has("totalResults") ? searchInfo.get("totalResults").asText() : "N/A"));
            }
            if (rootNode.has("context")) {
                JsonNode context = rootNode.get("context");
                System.out.println("  - CSE 이름: " + (context.has("title") ? context.get("title").asText() : "N/A"));
            }
            if (rootNode.has("queries")) {
                JsonNode queries = rootNode.get("queries");
                if (queries.has("request") && queries.get("request").isArray() && queries.get("request").size() > 0) {
                    JsonNode request = queries.get("request").get(0);
                    String searchTerms = request.has("searchTerms") ? request.get("searchTerms").asText() : "N/A";
                    System.out.println("  - 실제 검색어: " + searchTerms);
                    System.out.println("  - 입력 인코딩: " + (request.has("inputEncoding") ? request.get("inputEncoding").asText() : "N/A"));
                }
            }
            System.out.println();
            
            List<SearchResult> results = new ArrayList<>();
            
            // items 배열에서 결과 추출
            JsonNode itemsNode = rootNode.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
                System.out.println("[검색 성공] " + itemsNode.size() + "개의 결과를 찾았습니다.");
                System.out.println("\n[검색 결과 상세]");
                
                for (int i = 0; i < itemsNode.size(); i++) {
                    JsonNode item = itemsNode.get(i);
                    String title = item.has("title") ? item.get("title").asText() : "";
                    String link = item.has("link") ? item.get("link").asText() : "";
                    String snippet = item.has("snippet") ? item.get("snippet").asText() : "";
                    
                    System.out.println("  [" + (i + 1) + "] " + title);
                    System.out.println("      링크: " + link);
                    System.out.println("      내용: " + (snippet.length() > 100 ? snippet.substring(0, 100) + "..." : snippet));
                    System.out.println();
                    
                    results.add(new SearchResult(title, link, snippet));
                }
            } else {
                System.out.println("[검색 결과] 검색 결과가 없습니다.");
            }
            
            System.out.println("[최종 결과] 총 " + results.size() + "개의 검색 결과를 반환합니다.");
            System.out.println("=====================================\n");
            
            return results;
            
        } catch (Exception e) {
            System.err.println("[검색 오류] Google Custom Search API 호출 중 오류 발생: " + e.getMessage());
            System.err.println("[오류 상세] " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.out.println("=====================================\n");
            return new ArrayList<>();
        }
    }
    
    /**
     * 검색 결과를 포맷팅된 문자열로 반환합니다.
     * GPT 프롬프트에 포함하기 적합한 형식입니다.
     */
    public String formatSearchResults(String query, int numResults) {
        System.out.println("[포맷팅 시작] 검색 결과를 GPT 프롬프트 형식으로 변환합니다.");
        System.out.println("[검색 쿼리] " + query);
        
        // 검색 수행
        List<SearchResult> results = search(query, numResults);
        
        if (results.isEmpty()) {
            System.out.println("[포맷팅 완료] 검색 결과가 없습니다.");
            return "검색 결과를 찾을 수 없습니다.";
        }
        
        // 제외 도메인 필터링
        List<SearchResult> filteredResults = new ArrayList<>();
        for (SearchResult result : results) {
            boolean shouldExclude = false;
            
            // 제외 도메인 체크
            if (excludedDomainsList != null && !excludedDomainsList.isEmpty()) {
                String linkLower = result.getLink().toLowerCase();
                for (String excludedDomain : excludedDomainsList) {
                    if (linkLower.contains(excludedDomain)) {
                        shouldExclude = true;
                        System.out.println("[필터링] 제외 도메인으로 인해 제외: " + result.getTitle() + " (" + excludedDomain + ")");
                        break;
                    }
                }
            }
            
            // "Untitled" 제목 제외
            if (result.getTitle() != null && result.getTitle().trim().equalsIgnoreCase("Untitled")) {
                shouldExclude = true;
                System.out.println("[필터링] Untitled 제목으로 인해 제외: " + result.getLink());
            }
            
            if (!shouldExclude) {
                filteredResults.add(result);
            }
        }
        
        System.out.println("[필터링 결과] " + results.size() + "개 중 " + filteredResults.size() + "개 사용");
        
        if (filteredResults.isEmpty()) {
            System.out.println("[포맷팅 완료] 필터링 후 결과가 없습니다.");
            return "검색 결과를 찾을 수 없거나 모든 결과가 필터링되었습니다.";
        }
        
        // 포맷팅
        StringBuilder formatted = new StringBuilder();
        formatted.append("다음은 '").append(query).append("'에 대한 웹 검색 결과입니다:\n\n");
        
        for (int i = 0; i < filteredResults.size(); i++) {
            SearchResult result = filteredResults.get(i);
            formatted.append("[").append(i + 1).append("] ").append(result.getTitle()).append("\n");
            formatted.append("    링크: ").append(result.getLink()).append("\n");
            formatted.append("    내용: ").append(result.getSnippet()).append("\n\n");
        }
        
        System.out.println("[포맷팅 완료] " + filteredResults.size() + "개의 검색 결과를 포함한 프롬프트를 생성했습니다.");
        System.out.println("[프롬프트 길이] " + formatted.length() + "자");
        
        return formatted.toString();
    }
    
    /**
     * 검색 결과를 담는 내부 클래스
     */
    public static class SearchResult {
        private final String title;
        private final String link;
        private final String snippet;
        
        public SearchResult(String title, String link, String snippet) {
            this.title = title;
            this.link = link;
            this.snippet = snippet;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getLink() {
            return link;
        }
        
        public String getSnippet() {
            return snippet;
        }
    }
}
