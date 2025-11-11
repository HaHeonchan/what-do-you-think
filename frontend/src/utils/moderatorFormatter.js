/**
 * 모더레이터 메시지를 사람이 읽기 쉬운 형식으로 변환
 */
export const formatModeratorMessage = (message) => {
  if (!message) return message;

  // 역할 이름 매핑
  const roleNames = {
    creator: "생성자",
    critic: "비판자",
    analyst: "분석가",
    researcher: "웹 검색",
    summarizer: "요약자",
  };

  try {
    // JSON 형식인지 확인
    let jsonText = message.trim();
    
    // ```json ... ``` 형태로 감싸져 있는 경우
    if (jsonText.includes("```json")) {
      jsonText = jsonText.substring(
        jsonText.indexOf("```json") + 7,
        jsonText.lastIndexOf("```")
      ).trim();
    } else if (jsonText.includes("```")) {
      jsonText = jsonText.substring(
        jsonText.indexOf("```") + 3,
        jsonText.lastIndexOf("```")
      ).trim();
    }
    
    // JSON 파싱 시도
    const data = JSON.parse(jsonText);
    
    // ModeratorResponseDTO 형식인지 확인
    if (data.request !== undefined || data.shouldEnd !== undefined) {
      let formatted = "";
      
      // 종료 여부 확인
      if (data.shouldEnd === true) {
        formatted += "✅ 대화를 종료합니다.\n\n";
        formatted += "충분히 논의가 완료되어 요약자에게 요청합니다.";
        return formatted;
      }
      
      // 요청 목록이 있는 경우
      if (data.request && Array.isArray(data.request) && data.request.length > 0) {
        formatted += "📋 다음 역할들에게 질문을 보냅니다:\n\n";
        
        data.request.forEach((item, index) => {
          const roleName = roleNames[item.roleKey] || item.roleKey;
          formatted += `${index + 1}. ${roleName}\n`;
          
          if (item.messages) {
            // researcher인 경우 검색어로 표시
            if (item.roleKey === "researcher") {
              formatted += `   🔍 검색어: "${item.messages}"\n`;
            } else {
              formatted += `   💬 질문: ${item.messages}\n`;
            }
          }
          formatted += "\n";
        });
        
        return formatted.trim();
      } else {
        // 요청이 없는 경우
        return "대화를 계속 진행합니다.";
      }
    }
    
    // JSON이지만 ModeratorResponseDTO 형식이 아닌 경우 원본 반환
    return message;
  } catch (e) {
    // JSON 파싱 실패 시 원본 메시지 반환
    return message;
  }
};

