package com.myworld.amaray.dto.doc;

import java.util.List;

public record DocNode(DocDto doc, List<DocNode> children) {
}