### Usage

   ```bash
   usage: commercetools-project-sync
    -f,--full                           By default, a delta sync runs using
                                        last-sync-timestamp logic. Use this
                                        flag to run a full sync. i.e. sync
                                        the entire data set.
    -h,--help                           Print help information.
    -r,--runnerName <arg>               Choose a name for the running sync
                                        instance. Please make sure the name
                                        is unique, otherwise running more
                                        than 1 sync instance with the same
                                        name would lead to an unexpected
                                        behaviour. (optional parameter)
                                        default: 'runnerName'.
    -s,--sync <args>                    Choose one or more of the following modules
                                        to run: "types", "productTypes",
                                        "cartDiscounts", "customObjects",
                                        "categories", "products",
                                        "inventoryEntries", "states",
                                        "taxCategories", "customers",
                                        "shoppingLists" or "all".
       --syncProjectSyncCustomObjects   Sync custom objects that were created
                                        with project sync (this application).
       --productQueryParameters         Pass your customized product fetch limit
                                        and a product projection predicate to filter  
                                        product resources to sync in the JSON format. 
                                        Example: "{\"limit\": 100, \"where\": \"
                                        published=true\"}" could be used to fetch 
                                        only published products to sync and limit 
                                        max 100 elements in one page.                
    -v,--version                        Print the version of the application.
   ```
