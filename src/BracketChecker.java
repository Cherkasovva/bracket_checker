import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class BracketChecker {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java BracketChecker <config_file.json> <file_to_check>");
            System.exit(1); // принудительный выход
        }

        String configFile = args[0];
        String fileToCheck = args[1];

        try {
            // Чтение конфигурации скобок
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(new File(configFile), new TypeReference<>() {});
            List<Map<String, String>> bracketsConfig = (List<Map<String, String>>) config.get("bracket");

            // Создание маппинга открывающих и закрывающих скобок
            Map<Character, Character> bracketPairs = new HashMap<>();
            for (Map<String, String> bracket : bracketsConfig) {
                String left = bracket.get("left");
                String right = bracket.get("right");
                if (left.length() != 1 || right.length() != 1) {
                    throw new IllegalArgumentException("Brackets must be single characters");
                }
                bracketPairs.put(left.charAt(0), right.charAt(0));
            }

            // Чтение файла всего содержимого файла в строку для проверки
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fileToCheck)));

            // Проверка скобок
            checkBrackets(content, bracketPairs);

            System.out.println("Проверка завершена успешно: все скобки расставлены правильно.");
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка в конфигурации: " + e.getMessage());
        } catch (BracketMismatchException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void checkBrackets(String content, Map<Character, Character> bracketPairs) throws BracketMismatchException {
        Stack<BracketInfo> stack = new Stack<>();
        // стек хранит информацию об открывающих скобках, которые ещё не были закрыты

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            // charAt - возвращает указанный символ по индексу

            // Проверяем, является ли символ открывающей скобкой
            if (bracketPairs.containsKey(c)) {
                stack.push(new BracketInfo(c, i));
            } 
            // Проверяем, является ли символ закрывающей скобкой
            else if (bracketPairs.containsValue(c)) {
                if (stack.isEmpty()) {
                    throw new BracketMismatchException(String.format(
                            "Ошибка: Неожиданная закрывающая скобка '%c' на позиции %d", c, i));
                }

                BracketInfo top = stack.pop(); // извлекает из стека последнюю добавленную открывающую скобку
                char expectedClosing = bracketPairs.get(top.bracket);
                // bracket - поле из BracketInfo
                if (c != expectedClosing) {
                    throw new BracketMismatchException(String.format(
                            "Ошибка: Ожидалась закрывающая скобка '%c' для открывающей '%c' на позиции %d, но найдена '%c' на позиции %d",
                            expectedClosing, top.bracket, top.position, c, i));
                }
            }
        }

        if (!stack.isEmpty()) {
            BracketInfo unmatched = stack.pop();
            throw new BracketMismatchException(String.format(
                    "Ошибка: Не закрыта скобка '%c' на позиции %d", unmatched.bracket, unmatched.position));
        }
    }

    private static class BracketInfo {
        char bracket;
        int position;

        BracketInfo(char bracket, int position) {
            this.bracket = bracket;
            this.position = position;
        }
    }

    private static class BracketMismatchException extends Exception {
        public BracketMismatchException(String message) {
            super(message);
        }
    }
}