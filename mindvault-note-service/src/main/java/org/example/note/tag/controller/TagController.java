package org.example.note.tag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.note.tag.TagService;
import org.example.note.tag.dto.CreateTagDTO;
import org.example.note.tag.dto.DeleteTagDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;

    @PostMapping("/tag/create")
    public Result<?> createTag(@Valid @RequestBody CreateTagDTO createTagDTO) {
        return Result.success(tagService.createTag(createTagDTO));
    }

    @GetMapping("/tag/list")
    public Result<?> listTag() {
        return Result.success(tagService.listTags());
    }

    @DeleteMapping("/tag/delete")
    public Result<?> deleteTag(@Valid DeleteTagDTO deleteTagDTO) {
        tagService.deleteTag(deleteTagDTO);
        return Result.success();
    }
}