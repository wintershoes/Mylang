先识别import statement

然后再识别statement，一但识别到了第一个statement后，就不再识别import statement了



String[] token = grammarRule.matchToken(sanitizedWord);grammarRule.matchToken返回的是一个String[]{tokenType, word};这样一个含有两个元素的字符串数组，现在需要修改上面的表达式，在matchToken返回的数组后面加上一个