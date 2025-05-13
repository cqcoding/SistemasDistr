package com;

public class StopwordClassificador {

    public static boolean ehProvavelStopword(String palavra, int frequencia, int numPaginas) {
        palavra = palavra.toLowerCase();

        if (palavra.length() <= 3) return true;
        if (frequencia >= 100) return true;
        if (numPaginas >= 5 && frequencia > 30) return true;

        String[] palavrasComuns = {"a", "an", "the", "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
                                "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
                                "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
                                "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these",
                                "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has",
                                "had", "having", "do", "does", "did", "doing", "and", "but", "if", "or",
                                "because", "as", "until", "while", "of", "at", "by", "for", "with", "about",
                                "against", "between", "into", "through", "during", "before", "after", "above",
                                "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under",
                                "again", "further", "then", "once", "here", "there", "when", "where", "why",
                                "how", "all", "any", "both", "each", "few", "more", "most", "other", "some",
                                "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
                                "s", "t", "can", "will", "just", "don", "should", "now", "o", "as", "os", "um",
                                "uma", "uns", "umas", "de", "do", "da", "dos", "das", "em", "no", "na", "nos",
                                "nas", "com", "por", "pelo", "pela", "pelos", "pelas", "para", "e", "mas", "ou",
                                "que", "se", "ser", "sou", "é", "somos", "são", "era", "fui", "foste", "foi",
                                "fomos", "fostes", "foram", "estar", "estou", "está", "estamos", "estão",
                                "esteve", "estive", "estiveste", "estivemos", "estiveram", "ter", "tenho", "tem",
                                "temos", "têm", "tinha", "tive", "tiveste", "teve", "tivemos", "tiveram", "haver",
                                "hei", "há", "havemos", "hão", "houve", "meu", "minha", "meus", "minhas", "teu",
                                "tua", "teus", "tuas", "seu", "sua", "seus", "suas", "nosso", "nossa", "nossos",
                                "nossas", "vosso", "vossa", "vossos", "vossas", "ele", "ela", "eles", "elas",
                                "eu", "tu", "nós", "vós", "te", "lhe", "vos", "lhes", "mim", "ti", "si", "este",
                                "esta", "estes", "estas", "esse", "essa", "esses", "essas", "aquele", "aquela",
                                "aqueles", "aquelas", "isto", "isso", "aquilo", "aqui", "aí", "ali", "lá", "cá",
                                "muito", "mais", "menos", "bem", "mal", "já", "ainda", "agora", "sempre", "nunca",
                                "como", "quando", "onde", "porquê", "qual", "quanto", "também", "assim", "então",
                                "depois", "antes", "até", "sobre", "contra", "entre", "u"};
                                
        for (String comum : palavrasComuns) {
            if (palavra.equals(comum)) return true;
        }

        return false;
    }
}