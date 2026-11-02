package com.myworld.amaray.dto.common;

import java.util.List;

public record TagNode(TagDto tag, List<TagNode> children) {
}
