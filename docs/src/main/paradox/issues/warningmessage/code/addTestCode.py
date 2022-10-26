def start_processor(processor_name, **kwargs):
    document_id = kwargs.get('document_id')
    document_type = kwargs.get('document_type')
    version = kwargs.get('version')
    input_file_path_list = kwargs.get('input_file_path_list')
    output_dir_path = kwargs.get('output_dir_path')
    output_file_path = kwargs.get('output_file_path')
    context = kwargs.get("context")
    project_name = kwargs.get("project_name")
    extraction_config_file = kwargs.get("extraction_config_file")
    rules_filepath = kwargs.get("rules_filepath")
    document_names = kwargs.get("document_names")

    if processor_name == ProcessorName.PAGE_MAKER_PROCESSOR.value:
        from page_maker_processor.page_maker_main import start_page_maker_processor
        start_page_maker_processor(document_id, document_type, input_file_path_list, output_dir_path)
    elif processor_name == ProcessorName.PDF_MAKER_PROCESSOR.value:
        from pdf_maker_processor.pdf_maker_main import start_pdf_maker_processor
        start_pdf_maker_processor(input_file_path_list, output_file_path, version)
    elif processor_name == ProcessorName.OCR_PROCESSOR.value:
        from ocr_processor.ocr_main import start_ocr_processor
        start_ocr_processor(input_file_path_list, output_dir_path, version, context)
    elif processor_name == ProcessorName.EXTRACTION_PROCESSOR.value:
        print("*"*100, file=sys.stderr)
        from extraction_processor.extraction_main import start_extraction_processor
        start_extraction_processor(input_file_path_list, output_file_path, project_name, version, context,
                                   extraction_config_file,rules_filepath)