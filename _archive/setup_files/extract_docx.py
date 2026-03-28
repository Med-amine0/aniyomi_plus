import zipfile
import xml.etree.ElementTree as ET

def get_docx_text(path):
    document = zipfile.ZipFile(path)
    xml_content = document.read('word/document.xml')
    document.close()
    
    tree = ET.XML(xml_content)
    WORD_NAMESPACE = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    
    paragraphs = []
    for paragraph in tree.iter(WORD_NAMESPACE + 'p'):
        texts = [node.text
                 for node in paragraph.iter(WORD_NAMESPACE + 't')
                 if node.text]
        if texts:
            paragraphs.append(''.join(texts))
            
    return '\n'.join(paragraphs)

with open('brief_utf8.txt', 'w', encoding='utf-8') as f:
    f.write(get_docx_text('aniyomi_surgical_brief.docx'))
