import { createContext, useContext, useState } from 'react';

const ChatProcessingContext = createContext(null);

export const useChatProcessing = () => {
  const context = useContext(ChatProcessingContext);
  if (!context) {
    throw new Error('useChatProcessing must be used within ChatProcessingProvider');
  }
  return context;
};

export const ChatProcessingProvider = ({ children }) => {
  const [isProcessing, setIsProcessing] = useState(false);

  return (
    <ChatProcessingContext.Provider value={{ isProcessing, setIsProcessing }}>
      {children}
    </ChatProcessingContext.Provider>
  );
};

