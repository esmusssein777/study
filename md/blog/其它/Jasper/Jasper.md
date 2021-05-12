JasperReports是功能强大的开源报告工具，能够将丰富的内容传递到屏幕，打印机或PDF，HTML，XLS，RTF，ODT，CSV，TXT和XML文件中。它完全用Java编写，可以在各种启用Java的应用程序中使用以生成动态内容。它的主要目的是帮助您以简单灵活的方式创建面向页面的可打印文档。

表示报告设计的JRXML文件的编译是由[JasperCompileManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperCompileManager.html)类公开的compileReport（）方法执行的。通过编译，将报表设计加载到报表设计对象中，然后将其序列化并存储在磁盘上（[JasperReport](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperReport.html)类）。当应用程序希望用数据填充指定的报表设计时，将使用此序列化的对象。

为了填充报告设计，可以使用[JasperFillManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperFillManager.html)类公开的fillReportXXX（）方法。这些方法以序列化的形式接收报告设计对象或代表指定报告设计对象的文件作为参数，并接收与数据库的JDBC连接，从数据库中检索数据以填充报告。结果是一个对象，该对象表示准备打印的文档（[JasperPrint](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperPrint.html)类），并且可以序列化的形式存储在磁盘上（以备后用），可以传送到打印机或屏幕上，或者可以导出转换成PDF，HTML，XLS，RTF，ODT，CSV，TXT或XML文档。

如您所见，使用JasperReports时要使用的主要类是：

- [net.sf.jasperreports.engine.JasperCompileManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperCompileManager.html)
- [net.sf.jasperreports.engine.JasperFillManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperFillManager.html)
- [net.sf.jasperreports.engine.JasperPrintManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperPrintManager.html)
- [net.sf.jasperreports.engine.JasperExportManager](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/JasperExportManager.html)

这些类代表JasperReports引擎的外观。它们具有各种静态方法，可简化对API功能的访问，并可用于编译JRXML报表设计，填充报表，打印报表或导出为其他文档格式（PDF，HTML，XML）。

除了这些外观类之外，如果需要将报告导出为XLS，RTF，ODT，TXT或JasperExportManager中没有相应帮助方法的其他文档格式，您还可以直接与特定的导出器类一起使用。 ，或者需要配置导出过程并使之适应您的特定需求时。这些导出器实现可在JasperReports库的[net.sf.jasperreports.engine.export](http://jasperreports.sourceforge.net/api/net/sf/jasperreports/engine/export/package-summary.html)包中找到。



报告的基本构成要素是元素。元素是图形对象，例如文本字符串或矩形。在Jaspersoft Studio中，不存在行或段落的概念，就像在文字处理程序中一样。一切都是通过元素创建的，这些元素可以包含文本，创建表，显示图像等。此方法遵循大多数报表创作工具使用的模型。

Jaspersoft Studio依赖于提供的九个基本元素。 JasperReports库：

* Line
* Rectangle
* Ellipse
* Static text
*  Text field (or simply Field)
* Image
* Frame
* Subreport
* Crosstab
* Chart
* Break

通过这些元素的组合，可以生成各种报告。JasperReports还允许开发人员实现自己的通用元素和自定义组件，可以在Jaspersoft Studio中为其添加支持以创建适当的插件。

所有元素都有共同的属性，例如高度，宽度，位置以及它们所属的带。其他属性特定于元素的类型（例如，字体，如果是矩形，则为边框的粗细）。有几种类型。图形元素用于创建形状和显示图像（它们是线，矩形，椭圆形，图像）；文本元素用于打印文本字符串，例如标签或字段（它们是静态文本和文本字段）；frame元素用于对一组元素进行分组，并可以选择在它们周围绘制边框。子报表，图表和交叉表是更复杂的元素，本章稍后将进行讨论。有关详细信息，请参见单独的章节。最后，有一个特殊的元素用于插入固定的页面或分栏符。

元素被插入带中，并且每个元素与其带都密不可分。如果元素未完全包含在其范围内，则报表编译器将返回有关该元素位置的消息；否则，将返回错误消息。尽管存在此错误，仍会编译报告，在最坏的情况下，根本不会打印该元素。



定位元素

grid：参考点来定位和对齐页面中的元素

Bands：定义元素位置的顶部和左侧值始终相对于父容器，该父容器通常为band，但可以为框架元素。