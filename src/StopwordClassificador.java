public class StopwordClassificador {

    public static boolean ehProvavelStopword(String palavra, int frequencia, int numPaginas) {
        palavra = palavra.toLowerCase();

        if (palavra.length() <= 3) return true;
        if (frequencia >= 100) return true;
        if (numPaginas >= 5 && frequencia > 30) return true;

        String[] palavrasComuns = {"de", "e", "do", "da", "em", "o", "a", "para", "no", "na", "por", "com"};
        for (String comum : palavrasComuns) {
            if (palavra.equals(comum)) return true;
        }

        return false;
    }
}
