/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.langserver.completions.resolvers;

import org.ballerinalang.langserver.TextDocumentServiceContext;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.langserver.completions.consts.ItemResolverConstants;
import org.ballerinalang.langserver.completions.consts.Priority;
import org.ballerinalang.langserver.completions.consts.Snippet;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertTextFormat;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceContextResolver.
 */
public class ServiceContextResolver extends AbstractItemResolver {

    @Override
    public ArrayList<CompletionItem> resolveItems(TextDocumentServiceContext completionContext) {
        ArrayList<CompletionItem> completionItems = new ArrayList<>();
        // TODO: Add annotations
        this.addResourceCompletionItem(completionItems);
        this.addTypes(completionItems, completionContext.get(CompletionKeys.VISIBLE_SYMBOLS_KEY));
        return completionItems;
    }

    private void addResourceCompletionItem(List<CompletionItem> completionItems) {
        CompletionItem resource = new CompletionItem();
        resource.setLabel(ItemResolverConstants.RESOURCE_TYPE);
        resource.setInsertText(Snippet.RESOURCE.toString());
        resource.setInsertTextFormat(InsertTextFormat.Snippet);
        resource.setDetail(ItemResolverConstants.SNIPPET_TYPE);
        resource.setSortText(Priority.PRIORITY7.name());
        completionItems.add(resource);
    }

    private void addTypes(List<CompletionItem> completionItems, List<SymbolInfo> symbols) {
        List<SymbolInfo> filteredSymbols = symbols.stream()
                .filter(symbolInfo -> symbolInfo.getScopeEntry().symbol instanceof BTypeSymbol)
                .collect(Collectors.toList());
        this.populateCompletionItemList(filteredSymbols, completionItems);
    }
}
