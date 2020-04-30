package com.github.daggerok.mvcblog

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

@Configuration
class CommonMarkConfig {

  @Bean
  fun parser(): Parser = Parser.builder().build()

  @Bean
  fun htmlRenderer(): HtmlRenderer = HtmlRenderer.builder().build()
}

@Service
class MarkdownToHtmlConverter(private val parser: Parser,
                              private val htmlRenderer: HtmlRenderer) : (String) -> String {

  override fun invoke(text: String): String = htmlRenderer.render(parser.parse(text))
}

@Service
class ResourceRenderer : (Resource) -> String {
  override fun invoke(resource: Resource): String = resource.inputStream.use { inputStream ->
    InputStreamReader(inputStream).use { inputStreamReader ->
      BufferedReader(inputStreamReader).use { bufferedReader ->
        bufferedReader.lines().collect(Collectors.joining("\n")) //(""))//
      }
    }
  }
}

@Service
class MarkdownRenderer(private val resourceLoader: ResourceLoader,
                       private val resourceReader: ResourceRenderer,
                       private val markdownToHtmlConverter: MarkdownToHtmlConverter) : (String) -> String {

  override fun invoke(markdownLocation: String) = markdownLocation.run {
    val resource = resourceLoader.getResource("classpath:/posts/$this")
    val markdown = resourceReader(resource)
    markdownToHtmlConverter(markdown);
  }
}

@Controller
class BlogPages(private val markdownRenderer: MarkdownRenderer) {

  @GetMapping
  fun homePageView() = "index"

  @GetMapping("/blog")
  fun postListView() = "blog/index"

  @GetMapping("/blog/{id}")
  fun blogPostView(@PathVariable("id") id: String, model: Model) = model.run {
    this["id"] = id
    this["html"] = markdownRenderer("2020-04-30-hello.md")
    "blog/post"
  }
}

@SpringBootApplication
class MvcBlogApplication

fun main(args: Array<String>) {
  runApplication<MvcBlogApplication>(*args)
}
