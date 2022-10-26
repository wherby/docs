def __extract_by_paragraph_match(document: dict, pattern: dict, rule_id: str, context: dict):
    #print(pattern,rule_id,context)
    result: list = []
    regex = re.compile(pattern.get("regex"),re.IGNORECASE)
    is_skip_title: bool =  pattern.get("isSkipTitle", False)
    is_skip_table: bool = pattern.get("isSkipTable", False)
    exclude_regex = re.compile(pattern.get("excludeRegex", None)) if pattern.get("excludeRegex") is not None else None

    score: float = pattern.get("score", 1.0)
    paragraph_list = extract_paragraph_list_from_document(document)
    result_count: int = 0
    document_id = document.get("document_id")
    document_type = document.get("document_type")
    if 'isCompanyNamePrioritized' in pattern:
        company_name = context.get('company_name', '')
        cn_extractor = CompanyNameExtractor([company_name], pattern['isCompanyNamePrioritized'])
    for (idx, paragraph) in enumerate(paragraph_list):
        # Skip it if it is a title
        if is_skip_title and is_paragraph_a_title(paragraph):
            continue
        # Skip it if it is a table
        if is_skip_table and is_paragraph_a_table(paragraph):
            continue
        paragraph_text = paragraph.get("text_content")
        if 'isCompanyNamePrioritized' in pattern:
            company_name_score = cn_extractor.calculate_score(paragraph_text)
        else:
            company_name_score = 0
        if exclude_regex is not None and exclude_regex.search(paragraph_text) is not None:
            continue
        if regex.search(paragraph_text) is not None:
            result.append(compose_result_item(
                paragraph_text, score+company_name_score, 0, "text", rule_id, result_count,
                document_id, document_type, paragraph_list, idx, context))
    if pattern.get("regex") =="Partnership":
        print("**"*10)
        print(pattern)
        print("***"*199)
        print(document)
        print("=="*100)
        print(len(paragraph_list))
        for para in paragraph_list:
            print("--"*10)
            print(para)
        print("***"*199)
        print(result)
    return result
