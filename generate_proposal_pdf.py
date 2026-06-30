from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_JUSTIFY, TA_CENTER
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer

src = Path('Network_Auditor_Project_Proposal.md')
dst = Path('Network_Auditor_Project_Proposal.pdf')
text = src.read_text(encoding='utf-8')

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='Heading1', parent=styles['Heading1'], fontName='Helvetica-Bold', fontSize=18, leading=22, alignment=TA_CENTER, spaceAfter=10, textColor=colors.HexColor('#1f4e79')))
styles.add(ParagraphStyle(name='Heading2', parent=styles['Heading2'], fontName='Helvetica-Bold', fontSize=13, leading=16, spaceBefore=10, spaceAfter=6, textColor=colors.HexColor('#2f5597')))
styles.add(ParagraphStyle(name='Heading3', parent=styles['Heading3'], fontName='Helvetica-Bold', fontSize=11, leading=14, spaceBefore=8, spaceAfter=4))
styles.add(ParagraphStyle(name='Body', parent=styles['BodyText'], fontName='Helvetica', fontSize=10, leading=13, alignment=TA_JUSTIFY, spaceAfter=6))
styles.add(ParagraphStyle(name='Bullet', parent=styles['BodyText'], fontName='Helvetica', fontSize=10, leading=12, leftIndent=18, bulletIndent=12, spaceAfter=3))

story = []
for line in text.splitlines():
    if not line.strip():
        story.append(Spacer(1, 6))
    elif line.startswith('# '):
        story.append(Paragraph(line[2:], styles['Heading1']))
    elif line.startswith('## '):
        story.append(Paragraph(line[3:], styles['Heading2']))
    elif line.startswith('### '):
        story.append(Paragraph(line[4:], styles['Heading3']))
    elif line.startswith('- '):
        story.append(Paragraph(line[2:], styles['Bullet']))
    else:
        story.append(Paragraph(line, styles['Body']))

pdf = SimpleDocTemplate(str(dst), pagesize=A4, leftMargin=54, rightMargin=54, topMargin=54, bottomMargin=54)
pdf.build(story)
print('Generated', dst.resolve())
