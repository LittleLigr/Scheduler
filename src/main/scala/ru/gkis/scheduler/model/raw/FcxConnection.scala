package ru.gkis.scheduler.model.raw

case class FcxConnection(
                            name:String,
                            adapter:String,
                            address: String,
                            port: Int,
                            database: String,
                            user: String,
                            password: String,
                            timeout: Int,
                            pooling: Boolean,
                            minPoolSize: Int,
                            maxPoolSize: Int,
                            commandTimeout: Int,
                            compatible: String,
                            krbsrvname: String
                        )
