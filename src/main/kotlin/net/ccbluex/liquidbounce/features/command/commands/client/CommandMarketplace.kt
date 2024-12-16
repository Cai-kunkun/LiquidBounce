/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.api.Marketplace
import net.ccbluex.liquidbounce.api.MarketplaceItemType
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.*

object CommandMarketplace : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("marketplace")
        .hub()
        .subcommand(subscribeCommand())
        .subcommand(unsubscribeCommand())
        .subcommand(list())
        .subcommand(item())
        .build()

    private fun subscribeCommand() = CommandBuilder
        .begin("subscribe")
        .handler { command, _ ->
        }.build()

    private fun unsubscribeCommand() = CommandBuilder.begin("unsubscribe")
        .handler { command, _ ->

        }.build()

    private fun list() = CommandBuilder.begin("list")
        .parameter(
            ParameterBuilder
                .begin<Int>("page")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, anies ->
            val page = anies.getOrNull(0) as? Int ?: 1

            val response = Marketplace.requestMarketplaceItems(page, 10)

            if (response.items.isEmpty()) {
                chat(command.result("noItems"))
                return@handler
            }

            for (item in response.items) {
                chat(
                    regular(
                        "ID: ${item.id} | Name: ${item.name} | Type: ${item.type} | Description: ${item.description}"
                    )
                )
            }
        }
        .build()

    private fun item() = CommandBuilder.begin("item")
        .hub()
        .subcommand(
            CommandBuilder
                .begin("create")
                .parameter(
                    ParameterBuilder
                        .begin<String>("name")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .required()
                        .build()
                )
                .parameter(
                    ParameterBuilder
                        .begin<String>("type")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .autocompletedWith { begin -> MarketplaceItemType.entries.map { it.name }.filter { it.startsWith(begin) } }
                        .required()
                        .build()
                )
                .parameter(
                    ParameterBuilder
                        .begin<String>("description")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .vararg()
                        .required()
                        .build()
                )
                .handler { command, args ->
                    val name = args[0] as String
                    val type = args[1] as String
                    val description = (args[2] as Array<*>).joinToString(" ")

                    val response = Marketplace.createMarketplaceItem(name, MarketplaceItemType.valueOf(type), description)

                    chat(
                        regular(
                            command.result(
                                "itemCreated",
                                variable(response.id.toString()),
                                variable(response.name),
                                variable(response.type.name),
                                variable(response.description)
                            )
                        )
                    )
                }
                .build()
        )
        .subcommand(
            CommandBuilder
                .begin("edit")
                .parameter(
                    ParameterBuilder
                        .begin<Int>("id")
                        .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                        .required()
                        .build()
                )
                .parameter(
                    ParameterBuilder
                        .begin<String>("name")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .required()
                        .build()
                )
                .parameter(
                    ParameterBuilder
                        .begin<String>("type")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .autocompletedWith { begin -> MarketplaceItemType.entries.map { it.name }.filter { it.startsWith(begin) } }
                        .required()
                        .build()
                )
                .parameter(
                    ParameterBuilder
                        .begin<String>("description")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                        .vararg()
                        .required()
                        .build()
                )
                .handler { command, args ->
                    val id = args[0] as Int
                    val name = args[1] as String
                    val type = args[2] as String
                    val description = (args[3] as Array<*>).joinToString(" ")

                    val response = Marketplace.updateMarketplaceItem(id, name, MarketplaceItemType.valueOf(type), description)

                    chat(
                        regular(
                            command.result(
                                "itemEdited",
                                variable(response.id.toString()),
                                variable(response.name),
                                variable(response.type.name),
                                variable(response.description)
                            )
                        )
                    )
                }
                .build()
        )
        .subcommand(
            CommandBuilder
                .begin("delete")
                .parameter(
                    ParameterBuilder
                        .begin<Int>("id")
                        .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                        .required()
                        .build()
                )
                .handler { command, args ->
                    val id = args[0] as Int

                    Marketplace.deleteMarketplaceItem(id)

                    chat(regular(command.result("itemDeleted", variable(id.toString())))
                    )
                }
                .build()
        )
        .build()

}
